package com.lablink.cloudmind.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 AI 服务公共配置。
 *
 * <p>Embedding 和 Reranker 目前共用同一个 Python FastAPI 服务。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.ai.service")
public class AiServiceProperties {

    private String baseUrl = "http://localhost:9001";
}