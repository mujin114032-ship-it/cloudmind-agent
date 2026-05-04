package com.lablink.cloudmind.module.embedding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 Embedding 服务配置。
 *
 * <p>用于集中管理 FastAPI Embedding 服务地址、模型名称和向量维度。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.embedding.local")
public class EmbeddingProperties {

    private String baseUrl = "http://localhost:9001";

    private String model = "BAAI/bge-base-zh-v1.5";

    private Integer dimension = 768;

    private Integer batchSize = 32;
}
