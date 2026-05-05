package com.lablink.cloudmind.module.rag.dto;

import lombok.Data;

@Data
public class RagTraceQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 10L;

    private Long knowledgeBaseId;

    private Integer status;

    private String keyword;
}