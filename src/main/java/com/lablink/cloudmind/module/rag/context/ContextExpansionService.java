package com.lablink.cloudmind.module.rag.context;

import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;

import java.util.List;

public interface ContextExpansionService {

    /**
     * 对 Milvus 命中的 chunk 进行相邻上下文扩展。
     */
    List<RetrievedChunkVO> expand(List<RetrievedChunkVO> hitChunks);
}