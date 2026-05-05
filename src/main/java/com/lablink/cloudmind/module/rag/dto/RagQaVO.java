package com.lablink.cloudmind.module.rag.dto;

import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.Data;

import java.util.List;

/**
 * 普通 RAG 问答响应。
 */
@Data
public class RagQaVO {

    private String traceId;

    private String knowledgeBaseId;

    private String question;

    private String rewrittenQuestion;

    private String answer;

    private String modelName;

    private String promptVersion;

    private List<RetrievedChunkVO> references;

    private Long retrievalCostMs;

    private Long llmCostMs;

    private Long totalCostMs;
}