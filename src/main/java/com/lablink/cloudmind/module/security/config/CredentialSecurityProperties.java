package com.lablink.cloudmind.module.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户敏感凭证加密配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.security")
public class CredentialSecurityProperties {

    /**
     * 用户 API Key 加密密钥。
     *
     * <p>生产环境必须通过环境变量配置。</p>
     */
    private String credentialSecret;
}