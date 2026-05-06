package com.lablink.cloudmind.module.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 普通问答、SSE 问答、会话问答请求。
 */
@Data
public class RagQaRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer topK = 5;

    private Double scoreThreshold = 0.3;

    /**
     * Prompt 模板版本。
     *
     * <p>为空时使用后端默认版本。</p>
     */
    private String promptVersion;

    /**
     * 检索模式：fast / balanced / quality。
     *
     * <p>为空时使用后端默认模式。</p>
     */
    private String searchMode;
}