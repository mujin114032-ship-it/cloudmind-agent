package com.lablink.cloudmind.module.integration.lablink.security;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.integration.lablink.config.LabLinkIntegrationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * LabLink JWT 认证服务。
 *
 * <p>用于浏览器直接访问 CloudMind Agent 接口时，校验 LabLink 登录 Token。</p>
 */
@Component
@RequiredArgsConstructor
public class LabLinkJwtAuthService {

    private final LabLinkIntegrationProperties properties;

    public LabLinkUserPrincipal parseAuthorizationHeader(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少 LabLink 登录 Token");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "LabLink 登录 Token 为空");
        }

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(properties.getJwtSecret())
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = parseUserId(claims.get("userId"));

            Object usernameObj = claims.get("username");
            String username = usernameObj == null || !StringUtils.hasText(String.valueOf(usernameObj))
                    ? String.valueOf(userId)
                    : String.valueOf(usernameObj);

            Object roleObj = claims.get("role");
            String role = roleObj == null ? "" : String.valueOf(roleObj);

            return new LabLinkUserPrincipal(userId, username, role);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "LabLink Token 无效或已过期");
        }
    }

    private Long parseUserId(Object value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Token中缺少userId");
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Token中userId格式错误");
        }
    }
}