package com.lablink.cloudmind.module.reranker.dto;

import lombok.Data;

import java.util.List;

/**
 * Reranker 请求体。
 */
@Data
public class RerankRequest {

    private String query;

    private List<String> documents;

    private Integer topK;

    private Integer batchSize;
}