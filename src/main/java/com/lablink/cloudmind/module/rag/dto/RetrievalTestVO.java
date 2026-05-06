package com.lablink.cloudmind.module.rag.dto;

import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.Data;

import java.util.List;

/**
 * 知识库检索测试结果。
 */
@Data
public class RetrievalTestVO {

    private String knowledgeBaseId;

    private String query;

    private Integer topK;

    /**
     * Milvus 直接命中的 chunk 数。
     */
    private Integer hitCount;

    /**
     * 相邻扩展后的上下文 chunk 数。
     */
    private Integer contextCount;

    /**
     * 兼容原字段，当前等于 contextCount。
     */
    private Integer resultCount;

    private Long costMs;

    private List<RetrievedChunkVO> results;

    private String searchMode;
}