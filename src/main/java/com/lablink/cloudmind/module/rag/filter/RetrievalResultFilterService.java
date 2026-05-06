package com.lablink.cloudmind.module.rag.filter;

import com.lablink.cloudmind.module.rag.config.RagSearchOptions;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;

import java.util.List;

public interface RetrievalResultFilterService {

    /**
     * 对 Milvus 直接命中的 chunk 做过滤与去重。
     */
    List<RetrievedChunkVO> filterHitChunks(
            List<RetrievedChunkVO> hitChunks,
            RagSearchOptions options
    );

    /**
     * 对相邻扩展后的上下文 chunk 做最终去重与截断。
     */
    List<RetrievedChunkVO> filterContextChunks(
            List<RetrievedChunkVO> contextChunks,
            RagSearchOptions options
    );
}