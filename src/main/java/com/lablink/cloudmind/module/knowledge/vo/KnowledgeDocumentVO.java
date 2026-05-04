package com.lablink.cloudmind.module.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentVO {

    /**
     * 所有雪花 ID 返回前端时统一使用 string，避免 JS 精度丢失。
     */
    private String documentId;

    private String knowledgeBaseId;

    private String fileId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String parserType;

    private Integer parseStatus;

    private Integer ingestStatus;

    private Integer chunkCount;

    private String embeddingModel;

    private Integer embeddingDim;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;

    private String storageType;

    private String storagePath;

    private String contentType;
}