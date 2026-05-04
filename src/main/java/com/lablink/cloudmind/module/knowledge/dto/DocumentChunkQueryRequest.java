package com.lablink.cloudmind.module.knowledge.dto;

import lombok.Data;

@Data
public class DocumentChunkQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 20L;
}