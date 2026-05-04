package com.lablink.cloudmind.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddKnowledgeDocumentRequest {

    /**
     * 前端按 string 传递雪花 ID，避免 JS 精度丢失。
     */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    @Size(max = 50, message = "文件类型不能超过50个字符")
    private String fileType;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
}