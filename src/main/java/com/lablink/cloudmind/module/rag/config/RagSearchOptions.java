package com.lablink.cloudmind.module.rag.config;

import lombok.Data;

/**
 * 一次 RAG 检索实际生效的参数。
 */
@Data
public class RagSearchOptions {

    private String searchMode;

    private Integer topK;

    private Double scoreThreshold;

    private Integer vectorCandidateTopK;

    private Boolean keywordEnabled;

    private Integer keywordCandidateTopK;

    private Boolean rerankEnabled;

    private Boolean contextExpansionEnabled;

    private Integer contextWindowBefore;

    private Integer contextWindowAfter;

    private Integer maxContextChunks;

    private Integer maxHitChunksPerDocument;
}