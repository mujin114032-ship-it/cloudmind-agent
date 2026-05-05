package com.lablink.cloudmind.module.rag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagTraceVO {

    private String traceId;

    private String knowledgeBaseId;

    private Integer requestType;

    private String originalQuestion;

    private String finalQuestion;

    private Integer topK;

    private Double scoreThreshold;

    private Integer resultCount;

    private String modelName;

    private Long retrievalCostMs;

    private Long llmCostMs;

    private Long totalCostMs;

    private Integer status;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}