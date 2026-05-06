package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 普通 RAG 问答应用服务。
 *
 * <p>负责串联：问题清洗 → 检索 → Prompt 组装 → 大模型回答。</p>
 */
@Service
@RequiredArgsConstructor
public class RagQaApplicationService {

    private final QueryPreprocessor queryPreprocessor;

    private final RetrievalApplicationService retrievalApplicationService;

    private final RagTraceService ragTraceService;

    private final QueryRewriteService queryRewriteService;

    private final AnswerPostProcessor answerPostProcessor;

    private final RagPromptBuilder ragPromptBuilder;

    private final ChatClient chatClient;

    public RagQaVO qa(Long knowledgeBaseId, RagQaRequest request) {
        return doQa(knowledgeBaseId, request, null);
    }

    public RagQaVO qaWithHistory(Long knowledgeBaseId, RagQaRequest request, List<ChatMessage> historyMessages) {
        return doQa(knowledgeBaseId, request, historyMessages);
    }

    private RagQaVO doQa(Long knowledgeBaseId, RagQaRequest request, List<ChatMessage> historyMessages) {
        long totalStart = System.currentTimeMillis();
        Long traceId = null;

        try {
            String question = queryPreprocessor.preprocess(request.getQuestion());

            String rewrittenQuestion = historyMessages == null || historyMessages.isEmpty()
                    ? queryRewriteService.rewrite(question)
                    : queryRewriteService.rewriteWithHistory(question, historyMessages);

            int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
            double scoreThreshold = request.getScoreThreshold() == null ? 0.3 : request.getScoreThreshold();

            traceId = ragTraceService.startTrace(
                    knowledgeBaseId,
                    RagRequestTypeEnum.NORMAL.getCode(),
                    request.getQuestion(),
                    rewrittenQuestion,
                    topK,
                    scoreThreshold
            );

            RetrievalTestRequest retrievalRequest = new RetrievalTestRequest();
            retrievalRequest.setQuery(rewrittenQuestion);
            retrievalRequest.setTopK(topK);
            retrievalRequest.setScoreThreshold(scoreThreshold);

            long retrievalStart = System.currentTimeMillis();
            RetrievalTestVO retrievalResult = retrievalApplicationService.retrievalTest(knowledgeBaseId, retrievalRequest);
            long retrievalCostMs = System.currentTimeMillis() - retrievalStart;

            List<RetrievedChunkVO> references = retrievalResult.getResults();
            ragTraceService.recordRetrieval(traceId, references, retrievalCostMs);


            String promptVersion = ragPromptBuilder.resolvePromptVersion(request.getPromptVersion());

            String systemPrompt = ragPromptBuilder.buildSystemPrompt(promptVersion);
            String userPrompt = ragPromptBuilder.buildUserPrompt(
                    promptVersion,
                    question,
                    rewrittenQuestion,
                    references
            );

            ragTraceService.recordPrompt(traceId, promptVersion, systemPrompt, userPrompt);

            long llmStart = System.currentTimeMillis();
            String answer = chatClient.chat(systemPrompt, userPrompt);
            long llmCostMs = System.currentTimeMillis() - llmStart;

            String finalAnswer = answerPostProcessor.process(answer, references);
            long totalCostMs = System.currentTimeMillis() - totalStart;

            ragTraceService.markSuccess(
                    traceId,
                    chatClient.modelName(),
                    finalAnswer,
                    llmCostMs,
                    totalCostMs
            );

            RagQaVO vo = new RagQaVO();
            vo.setTraceId(String.valueOf(traceId));
            vo.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
            vo.setQuestion(question);
            vo.setRewrittenQuestion(rewrittenQuestion);
            vo.setAnswer(finalAnswer);
            vo.setModelName(chatClient.modelName());
            vo.setPromptVersion(promptVersion);
            vo.setReferences(references);
            vo.setRetrievalCostMs(retrievalCostMs);
            vo.setLlmCostMs(llmCostMs);
            vo.setTotalCostMs(totalCostMs);
            vo.setSearchMode(retrievalResult.getSearchMode());

            return vo;
        } catch (Exception ex) {
            if (traceId != null) {
                ragTraceService.markFailed(traceId, ex.getMessage(), System.currentTimeMillis() - totalStart);
            }
            throw ex;
        }
    }

}