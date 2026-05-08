package com.lablink.cloudmind.module.integration.lablink.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 从 LabLink JWT 中解析出的当前登录用户。
 */
@Data
@AllArgsConstructor
public class LabLinkUserPrincipal {

    private Long userId;

    private String username;

    private String role;
}