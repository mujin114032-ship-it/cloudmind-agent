package com.lablink.cloudmind.module.vector.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 即将写入 Milvus 的 chunk 向量行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkVectorRow {

    private Long chunkId;

    private Long knowledgeBaseId;

    private Long documentId;

    private Long userId;

    private Integer chunkIndex;

    private float[] embedding;
}