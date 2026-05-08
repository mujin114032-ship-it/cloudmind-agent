package com.lablink.cloudmind.module.integration.lablink.security;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.integration.lablink.config.LabLinkIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * LabLink 内部接口鉴权服务。
 *
 * <p>用于校验请求是否来自可信的 LabLink 后端。</p>
 */
@Component
@RequiredArgsConstructor
public class LabLinkInternalAuthService {

    private final LabLinkIntegrationProperties properties;

    public void verifyInternalToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少内部访问令牌");
        }

        if (!token.equals(properties.getInternalToken())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "内部访问令牌无效");
        }
    }
}