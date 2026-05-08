package com.lablink.cloudmind.module.integration.lablink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LabLink 集成配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.integration.lablink")
public class LabLinkIntegrationProperties {

    /**
     * LabLink 后端调用 CloudMind 内部接口时使用的服务间密钥。
     *
     * <p>注意：这不是用户登录 token，不应该暴露给前端。</p>
     */
    private String internalToken = "dev-cloudmind-lablink-token";

    /**
     * 自动创建私有知识库时使用的名称后缀。
     */
    private String defaultKnowledgeBaseSuffix = "的 LabLink 私有知识库";

    /**
     * 允许进入 Agent 知识库的文字类文件类型。
     */
    private List<String> supportedFileTypes = List.of("pdf", "doc", "docx", "txt", "md", "html");

    /**
     * LabLink JWT 签名密钥。
     *
     * <p>用于 CloudMind 直接验证浏览器携带的 LabLink 登录 Token。</p>
     */
    private String jwtSecret = "LabLinkAI#SecretKey$2026!@#";

    private Minio minio = new Minio();

    @Data
    public static class Minio {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucketName;
    }
}