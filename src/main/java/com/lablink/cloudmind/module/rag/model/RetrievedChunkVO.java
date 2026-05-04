package com.lablink.cloudmind.module.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * RAG 检索命中的文档片段。
 */
@Data
public class RetrievedChunkVO {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    private Double score;

    /**
     * 完整 chunk 文本，仅用于 Prompt，不返回前端。
     */
    @JsonIgnore
    private String chunkText;

    /**
     * 文本预览，用于前端展示。
     */
    private String textPreview;
}