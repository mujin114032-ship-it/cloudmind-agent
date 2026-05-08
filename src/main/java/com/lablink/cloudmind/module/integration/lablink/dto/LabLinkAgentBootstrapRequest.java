package com.lablink.cloudmind.module.integration.lablink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LabLink Agent 初始化请求。
 */
@Data
public class LabLinkAgentBootstrapRequest {

    /**
     * LabLink 用户 ID。
     *
     * <p>第一版直接作为 CloudMind userId 使用。</p>
     */
    @NotBlank(message = "LabLink用户ID不能为空")
    private String labLinkUserId;

    /**
     * LabLink 用户名，用于生成私有知识库展示名称。
     */
    private String username;
}