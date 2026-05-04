package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.rag.processor.QueryPreprocessor;
import com.lablink.cloudmind.module.rag.prompt.RagPromptBuilder;
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

        try {
            String question = queryPreprocessor.preprocess(request.getQuestion());

            sendEvent(emitter, "retrieval_start", Map.of(
                    "question", question
            ));

            RetrievalTestRequest retrievalRequest = new RetrievalTestRequest();
            retrievalRequest.setQuery(question);
            retrievalRequest.setTopK(request.getTopK());
            retrievalRequest.setScoreThreshold(request.getScoreThreshold());

            long retrievalStart = System.currentTimeMillis();
            RetrievalTestVO retrievalResult = retrievalApplicationService.retrievalTest(knowledgeBaseId, retrievalRequest);
            long retrievalCostMs = System.currentTimeMillis() - retrievalStart;

            List<RetrievedChunkVO> references = retrievalResult.getResults();

            Map<String, Object> retrievalDone = new LinkedHashMap<>();
            retrievalDone.put("question", question);
            retrievalDone.put("resultCount", references.size());
            retrievalDone.put("references", references);
            retrievalDone.put("retrievalCostMs", retrievalCostMs);
            sendEvent(emitter, "retrieval_done", retrievalDone);

            String systemPrompt = ragPromptBuilder.buildSystemPrompt();
            String userPrompt = ragPromptBuilder.buildUserPrompt(question, references);

            sendEvent(emitter, "answer_start", Map.of(
                    "modelName", chatClient.modelName()
            ));

            StringBuilder answerBuilder = new StringBuilder();

            long llmStart = System.currentTimeMillis();
            chatClient.streamChat(systemPrompt, userPrompt, delta -> {
                answerBuilder.append(delta);

                Map<String, Object> deltaData = new LinkedHashMap<>();
                deltaData.put("content", delta);

                sendEvent(emitter, "answer_delta", deltaData);
            });
            long llmCostMs = System.currentTimeMillis() - llmStart;

            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("answer", answerBuilder.toString().trim());
            doneData.put("modelName", chatClient.modelName());
            doneData.put("retrievalCostMs", retrievalCostMs);
            doneData.put("llmCostMs", llmCostMs);
            doneData.put("totalCostMs", System.currentTimeMillis() - totalStart);
            doneData.put("references", references);

            sendEvent(emitter, "answer_done", doneData);

            emitter.complete();
        } catch (Exception ex) {
            log.warn("SSE RAG 问答失败：{}", ex.getMessage(), ex);

            Map<String, Object> errorData = new LinkedHashMap<>();
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