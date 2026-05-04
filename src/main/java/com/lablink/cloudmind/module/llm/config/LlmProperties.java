package com.lablink.cloudmind.module.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型调用配置。
 *
 * <p>当前按 OpenAI-compatible API 设计，方便后续切换不同模型服务商。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.llm.openai-compatible")
public class LlmProperties {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Integer timeoutSeconds;
}