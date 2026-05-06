package com.lablink.cloudmind.module.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库检索测试请求。
 */
@Data
public class RetrievalTestRequest {

    @NotBlank(message = "检索问题不能为空")
    private String query;

    private Integer topK = 5;

    /**
     * 相似度阈值，低于该分数的结果会被过滤。
     */
    private Double scoreThreshold = 0.3;

    private String promptVersion;

    private String searchMode;
}