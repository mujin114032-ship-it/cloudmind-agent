package com.lablink.cloudmind.module.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.rag")
public class RagProperties {

    private Boolean queryRewriteEnabled = true;

    private Boolean chatHistoryRewriteEnabled = true;

    private Integer queryRewriteHistoryLimit = 6;

    private Integer queryRewriteMaxLength = 300;

    /**
     * 是否启用相邻 Chunk 扩展。
     */
    private Boolean contextExpansionEnabled = true;

    /**
     * 命中 chunk 前面补几个 chunk。
     */
    private Integer contextWindowBefore = 1;

    /**
     * 命中 chunk 后面补几个 chunk。
     */
    private Integer contextWindowAfter = 1;

    /**
     * 最终进入 Prompt 的最大上下文 chunk 数。
     */
    private Integer maxContextChunks = 20;

    /**
     * 系统级最低检索分数阈值。
     */
    private Double retrievalMinScoreThreshold = 0.2;

    /**
     * 是否开启重复 chunk 去重。
     */
    private Boolean removeDuplicateChunksEnabled = true;

    /**
     * 单篇文档最多保留几个向量直接命中 chunk。
     */
    private Integer maxHitChunksPerDocument = 3;

    /**
     * 是否启用历史使用门控。
     *
     * <p>开启后，只有当前问题存在指代或省略时，才结合历史消息改写。</p>
     */
    private Boolean queryRewriteHistoryGateEnabled = true;

    /**
     * 默认 Prompt 版本。
     *
     * <p>当前端没有指定 promptVersion 时使用。</p>
     */
    private String defaultPromptVersion = "basic-v1";

    /**
     * 是否启用回答后处理。
     */
    private Boolean answerPostProcessEnabled = true;
}