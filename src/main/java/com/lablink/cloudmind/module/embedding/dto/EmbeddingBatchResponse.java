package com.lablink.cloudmind.module.embedding.dto;

import lombok.Data;

import java.util.List;

/**
 * 本地 Embedding 服务的批量响应体。
 */
@Data
public class EmbeddingBatchResponse {

    private String model;

    private Integer dimension;

    private Integer count;

    private String device;

    private Long costMs;

    private List<List<Double>> embeddings;
}