package com.lablink.cloudmind.module.embedding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentIngestTaskVO {

    private String taskId;

    private String documentId;

    private String knowledgeBaseId;

    private Integer status;

    private Integer stage;

    private Integer progress;

    private Integer totalChunks;

    private Integer processedChunks;

    private String modelName;

    private Integer embeddingDim;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}