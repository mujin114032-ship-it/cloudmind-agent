package com.lablink.cloudmind.module.reranker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reranker 服务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.reranker.local")
public class RerankerProperties {

    private String baseUrl = "http://localhost:9001";

    private String model = "BAAI/bge-reranker-base";

    private Integer batchSize = 16;

    private Integer timeoutSeconds = 60;
}