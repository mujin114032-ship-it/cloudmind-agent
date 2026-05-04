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

    private Integer resultCount;

    private Long costMs;

    private List<RetrievedChunkVO> results;
}