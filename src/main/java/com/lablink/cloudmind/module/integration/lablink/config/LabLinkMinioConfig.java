package com.lablink.cloudmind.module.integration.lablink.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LabLink MinIO 客户端配置。
 *
 * <p>CloudMind 通过该客户端读取 LabLink 存储在 MinIO 中的文件。</p>
 */
@Configuration
@RequiredArgsConstructor
public class LabLinkMinioConfig {

    private final LabLinkIntegrationProperties properties;

    @Bean("labLinkMinioClient")
    public MinioClient labLinkMinioClient() {
        LabLinkIntegrationProperties.Minio minio = properties.getMinio();

        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}