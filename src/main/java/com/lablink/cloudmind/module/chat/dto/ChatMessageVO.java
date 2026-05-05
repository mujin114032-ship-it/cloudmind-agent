package com.lablink.cloudmind.module.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO {

    private String messageId;

    private String sessionId;

    private String role;

    private String content;

    private String traceId;

    private Integer status;

    private String errorMessage;

    private Long retrievalCostMs;

    private Long llmCostMs;

    private Long totalCostMs;

    private List<ChatMessageReferenceVO> references;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}