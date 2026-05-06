package com.lablink.cloudmind.module.embedding.config;

import com.lablink.cloudmind.module.ai.config.AiServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Embedding 服务 HTTP 客户端配置。
 *
 * <p>将 RestClient 独立配置为 Bean，避免业务客户端内部硬编码 HTTP 客户端构建逻辑。</p>
 */
@Configuration
public class EmbeddingRestClientConfig {

    @Bean
    public RestClient embeddingRestClient(
            RestClient.Builder restClientBuilder,
            AiServiceProperties aiServiceProperties
    ) {
        return restClientBuilder
                .baseUrl(aiServiceProperties.getBaseUrl())
                .build();
    }
}