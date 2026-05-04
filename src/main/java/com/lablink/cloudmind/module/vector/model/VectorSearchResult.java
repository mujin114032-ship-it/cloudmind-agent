package com.lablink.cloudmind.module.vector.model;

import lombok.Data;

/**
 * Milvus 向量检索结果。
 */
@Data
public class VectorSearchResult {

    private Long chunkId;

    private Long knowledgeBaseId;

    private Long documentId;

    private Long userId;

    private Integer chunkIndex;

    /**
     * Milvus 返回的相似度分数。
     *
     * <p>COSINE 场景下，分数越高通常表示越相似。</p>
     */
    private Double score;
}
