package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.rag.processor.QueryPreprocessor;
import com.lablink.cloudmind.module.rag.prompt.RagPromptBuilder;
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

    private final RagPromptBuilder ragPromptBuilder;

    private final ChatClient chatClient;

    public RagQaVO qa(Long knowledgeBaseId, RagQaRequest request) {
        long totalStart = System.currentTimeMillis();

        String question = queryPreprocessor.preprocess(request.getQuestion());

        RetrievalTestRequest retrievalRequest = new RetrievalTestRequest();
        retrievalRequest.setQuery(question);
        retrievalRequest.setTopK(request.getTopK());
        retrievalRequest.setScoreThreshold(request.getScoreThreshold());

        long retrievalStart = System.currentTimeMillis();
        RetrievalTestVO retrievalResult = retrievalApplicationService.retrievalTest(knowledgeBaseId, retrievalRequest);
        long retrievalCostMs = System.currentTimeMillis() - retrievalStart;

        List<RetrievedChunkVO> references = retrievalResult.getResults();

        String systemPrompt = ragPromptBuilder.buildSystemPrompt();
        String userPrompt = ragPromptBuilder.buildUserPrompt(question, references);

        long llmStart = System.currentTimeMillis();
        String answer = chatClient.chat(systemPrompt, userPrompt);
        long llmCostMs = System.currentTimeMillis() - llmStart;

        RagQaVO vo = new RagQaVO();
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
        vo.setQuestion(question);
        vo.setAnswer(postProcessAnswer(answer));
        vo.setModelName(chatClient.modelName());
        vo.setReferences(references);
        vo.setRetrievalCostMs(retrievalCostMs);
        vo.setLlmCostMs(llmCostMs);
        vo.setTotalCostMs(System.currentTimeMillis() - totalStart);

        return vo;
    }

    private String postProcessAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "大模型未返回有效回答。";
        }

        return answer.trim();
    }
}