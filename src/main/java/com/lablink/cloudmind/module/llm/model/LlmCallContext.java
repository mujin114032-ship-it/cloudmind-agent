package com.lablink.cloudmind.module.llm.model;

import lombok.Builder;
import lombok.Data;

/**
 * 单次大模型调用上下文。
 *
 * <p>用于 LabLink 私有 Agent 场景下，让每个用户使用自己的 API Key 和模型配置。</p>
 */
@Data
@Builder
public class LlmCallContext {

    private Long userId;

    private String provider;

    private String apiKey;

    private String modelName;

    private Integer timeoutSeconds;
}