package com.lablink.cloudmind.module.knowledge.vo;

import lombok.Data;

@Data
public class DocumentChunkVO {

    private String chunkId;

    private String documentId;

    private Integer chunkIndex;

    private String chunkText;

    private String chunkHash;

    private Integer tokenCount;

    private Integer charCount;

    private String embeddingModel;

    private Integer embeddingDim;

    private Integer enabled;
}