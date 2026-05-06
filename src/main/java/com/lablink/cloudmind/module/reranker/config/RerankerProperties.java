package com.lablink.cloudmind.module.reranker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reranker 模型配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.ai.reranker")
public class RerankerProperties {

    private String model = "BAAI/bge-reranker-base";

    private Integer batchSize = 16;

    private Integer timeoutSeconds = 60;
}