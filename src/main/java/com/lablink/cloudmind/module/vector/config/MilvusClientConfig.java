package com.lablink.cloudmind.module.vector.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端配置。
 */
@Configuration
public class MilvusClientConfig {

    @Bean
    public MilvusClientV2 milvusClientV2(MilvusProperties properties) {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(properties.getUri())
                .token(properties.getToken())
                .build();

        return new MilvusClientV2(connectConfig);
    }
}