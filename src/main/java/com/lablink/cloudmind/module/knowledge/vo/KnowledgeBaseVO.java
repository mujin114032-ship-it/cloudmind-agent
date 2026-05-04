package com.lablink.cloudmind.module.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseVO {

    /**
     * 前端使用字符串接收 ID，避免 JavaScript number 精度丢失。
     * 雪花 ID 超过 JS 安全整数范围，返回前端时统一使用 String。
     */
    private String id;

    private String name;

    private String description;

    private Integer visibility;

    private Integer status;

    private Integer documentCount;

    private Integer chunkCount;

    private String embeddingModel;

    private Integer embeddingDim;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}