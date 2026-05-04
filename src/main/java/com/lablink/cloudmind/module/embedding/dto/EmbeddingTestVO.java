package com.lablink.cloudmind.module.embedding.dto;

import lombok.Data;

/**
 * 文档分块向量化测试结果。
 */
@Data
public class EmbeddingTestVO {

    private String documentId;

    private Integer chunkCount;

    private Integer vectorCount;

    private String modelName;

    private Integer embeddingDim;

    private Long costMs;

    private Boolean success;
}