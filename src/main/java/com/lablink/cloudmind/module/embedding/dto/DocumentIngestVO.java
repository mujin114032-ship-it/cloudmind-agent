package com.lablink.cloudmind.module.embedding.dto;

import lombok.Data;

/**
 * 文档向量入库结果。
 */
@Data
public class DocumentIngestVO {

    private String documentId;

    private Integer chunkCount;

    private Integer vectorCount;

    private String modelName;

    private Integer embeddingDim;

    private Long costMs;

    private Integer ingestStatus;

    private Boolean success;
}
