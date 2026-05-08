package com.lablink.cloudmind.module.integration.lablink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LabLink 用户保存 LLM Key 请求。
 */
@Data
public class LabLinkSaveLlmKeyRequest {

    @NotBlank(message = "LabLink用户ID不能为空")
    private String labLinkUserId;

    private String username;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    private String modelName;
}