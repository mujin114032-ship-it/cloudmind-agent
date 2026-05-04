package com.lablink.cloudmind.module.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeBaseQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 10L;

    private String keyword;
}