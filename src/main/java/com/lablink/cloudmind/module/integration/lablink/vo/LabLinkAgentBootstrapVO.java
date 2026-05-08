package com.lablink.cloudmind.module.integration.lablink.vo;

import lombok.Data;

/**
 * LabLink Agent 初始化响应。
 */
@Data
public class LabLinkAgentBootstrapVO {

    /**
     * 是否已完成 Agent 初始化。
     *
     * <p>第一版要求：有私有知识库 + 有 LLM Key 才算 initialized。</p>
     */
    private Boolean initialized;

    private Boolean hasPrivateKnowledgeBase;

    private Boolean hasLlmKey;

    private String knowledgeBaseId;

    private String knowledgeBaseName;

    /**
     * 后续保存 Key 后返回，例如 sk-****abcd。
     */
    private String apiKeyMask;

    private String modelName;

    private String defaultPromptVersion;

    private String defaultSearchMode;

    /**
     * 历史文件懒同步状态。
     *
     * <p>第一版先返回 NOT_STARTED，后面再接懒同步。</p>
     */
    private String historySyncStatus;
}