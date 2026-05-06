package com.lablink.cloudmind.module.embedding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 模型配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.ai.embedding")
public class EmbeddingProperties {

    private String model = "BAAI/bge-base-zh-v1.5";

    private Integer dimension = 768;

    private Integer batchSize = 32;

    private Integer timeoutSeconds = 60;
}