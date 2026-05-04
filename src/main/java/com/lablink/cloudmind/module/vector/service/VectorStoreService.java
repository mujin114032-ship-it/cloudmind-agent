package com.lablink.cloudmind.module.vector.service;

import com.lablink.cloudmind.module.vector.model.ChunkVectorRow;
import com.lablink.cloudmind.module.vector.model.VectorSearchResult;

import java.util.List;

public interface VectorStoreService {

    /**
     * 初始化 Milvus Collection。
     */
    void initCollectionIfAbsent();

    /**
     * 批量写入 chunk 向量。
     */
    void insertChunkVectors(List<ChunkVectorRow> rows);

    List<VectorSearchResult> search(
            Long knowledgeBaseId,
            Long userId,
            float[] queryVector,
            Integer topK
    );
}