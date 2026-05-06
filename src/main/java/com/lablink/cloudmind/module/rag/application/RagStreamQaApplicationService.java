package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.enums.RagRequestTypeEnum;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.rag.processor.AnswerPostProcessor;
import com.lablink.cloudmind.module.rag.processor.QueryPreprocessor;
import com.lablink.cloudmind.module.rag.prompt.RagPromptBuilder;
import com.lablink.cloudmind.module.rag.rewrite.QueryRewriteService;
import com.lablink.cloudmind.module.rag.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * SSE 流式 RAG 问答应用服务。
 *
 * <p>负责串联：问题清洗 → 检索 → Prompt 构造 → 大模型流式生成 → SSE 推送。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagStreamQaApplicationService {

    private static final Long SSE_TIMEOUT = 0L;

    private final QueryPreprocessor queryPreprocessor;

    private final RetrievalApplicationService retrievalApplicationService;

    private final RagTraceService ragTraceService;

    private final QueryRewriteService queryRewriteService;

    private final AnswerPostProcessor answerPostProcessor;

    private final RagPromptBuilder ragPromptBuilder;

    private final ChatClient chatClient;

    @Qualifier("ragTaskExecutor")
    private final Executor ragTaskExecutor;

    public SseEmitter streamQa(Long knowledgeBaseId, RagQaRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        ragTaskExecutor.execute(() -> doStreamQa(knowledgeBaseId, request, emitter));

        return emitter;
    }

    private void doStreamQa(Long knowledgeBaseId, RagQaRequest request, SseEmitter emitter) {
        long totalStart = System.currentTimeMillis();
        Long traceId = null;

        try {
            String question = queryPreprocessor.preprocess(request.getQuestion());
            String rewrittenQuestion = queryRewriteService.rewrite(question);

            int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
            double scoreThreshold = request.getScoreThreshold() == null ? 0.3 : request.getScoreThreshold();

            traceId = ragTraceService.startTrace(
                    knowledgeBaseId,
                    RagRequestTypeEnum.STREAM.getCode(),
                    request.getQuestion(),
                    rewrittenQuestion,
                    topK,
                    scoreThreshold
            );

            // Lambda 中不能直接引用会被重新赋值的 traceId，因此这里转成稳定的字符串变量。
            String traceIdStr = String.valueOf(traceId);

            sendEvent(emitter, "retrieval_start", Map.of(
                    "traceId", String.valueOf(traceId),
                    "question", question
            ));

            RetrievalTestRequest retrievalRequest = new RetrievalTestRequest();
            retrievalRequest.setQuery(rewrittenQuestion);
            retrievalRequest.setTopK(topK);
            retrievalRequest.setScoreThreshold(scoreThreshold);

            long retrievalStart = System.currentTimeMillis();
            RetrievalTestVO retrievalResult = retrievalApplicationService.retrievalTest(knowledgeBaseId, retrievalRequest);
            long retrievalCostMs = System.currentTimeMillis() - retrievalStart;

            List<RetrievedChunkVO> references = retrievalResult.getResults();
            ragTraceService.recordRetrieval(traceId, references, retrievalCostMs);

            Map<String, Object> retrievalDone = new LinkedHashMap<>();
            retrievalDone.put("traceId", traceIdStr);
            retrievalDone.put("question", question);
            retrievalDone.put("rewrittenQuestion", rewrittenQuestion);
            retrievalDone.put("resultCount", references.size());
            retrievalDone.put("references", references);
            retrievalDone.put("retrievalCostMs", retrievalCostMs);
            retrievalDone.put("searchMode", retrievalResult.getSearchMode());
            sendEvent(emitter, "retrieval_done", retrievalDone);

            String promptVersion = ragPromptBuilder.resolvePromptVersion(request.getPromptVersion());

            String systemPrompt = ragPromptBuilder.buildSystemPrompt(promptVersion);
            String userPrompt = ragPromptBuilder.buildUserPrompt(
                    promptVersion,
                    question,
                    rewrittenQuestion,
                    references
            );

            ragTraceService.recordPrompt(traceId, promptVersion, systemPrompt, userPrompt);

            sendEvent(emitter, "answer_start", Map.of(
                    "traceId", traceIdStr,
                    "modelName", chatClient.modelName(),
                    "promptVersion", promptVersion
            ));

            StringBuilder answerBuilder = new StringBuilder();

            long llmStart = System.currentTimeMillis();
            chatClient.streamChat(systemPrompt, userPrompt, delta -> {
                answerBuilder.append(delta);

                Map<String, Object> deltaData = new LinkedHashMap<>();
                deltaData.put("traceId", traceIdStr);
                deltaData.put("content", delta);

                sendEvent(emitter, "answer_delta", deltaData);
            });
            long llmCostMs = System.currentTimeMillis() - llmStart;

            String rawAnswer = answerBuilder.toString().trim();
            String finalAnswer = answerPostProcessor.process(rawAnswer, references);
            long totalCostMs = System.currentTimeMillis() - totalStart;

            ragTraceService.markSuccess(
                    traceId,
                    chatClient.modelName(),
                    finalAnswer,
                    llmCostMs,
                    totalCostMs
            );

            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("traceId", traceIdStr);
            doneData.put("answer", finalAnswer);
            doneData.put("modelName", chatClient.modelName());
            doneData.put("promptVersion", promptVersion);
            doneData.put("retrievalCostMs", retrievalCostMs);
            doneData.put("llmCostMs", llmCostMs);
            doneData.put("totalCostMs", totalCostMs);
            doneData.put("references", references);
            doneData.put("searchMode", retrievalResult.getSearchMode());

            sendEvent(emitter, "answer_done", doneData);

            emitter.complete();
        } catch (Exception ex) {
            log.warn("SSE RAG 问答失败：{}", ex.getMessage(), ex);

            if (traceId != null) {
                ragTraceService.markFailed(traceId, ex.getMessage(), System.currentTimeMillis() - totalStart);
            }

            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("traceId", traceId == null ? null : String.valueOf(traceId));
            errorData.put("message", ex.getMessage());

            sendEvent(emitter, "error", errorData);
            emitter.complete();
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException ex) {
            throw new RuntimeException("SSE 推送失败：" + ex.getMessage(), ex);
        }
    }
}