package com.lablink.cloudmind.module.reranker.dto;

import lombok.Data;

import java.util.List;

/**
 * Reranker 响应体。
 */
@Data
public class RerankResponse {

    private String model;

    private String device;

    private Integer count;

    private Long costMs;

    private List<RerankItem> results;

    @Data
    public static class RerankItem {
        private Integer index;
        private Double score;
    }
}