package com.lablink.cloudmind.module.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 普通 RAG 问答请求。
 */
@Data
public class RagQaRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer topK = 5;

    private Double scoreThreshold = 0.3;
}