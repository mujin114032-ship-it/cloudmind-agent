package com.lablink.cloudmind.module.integration.lablink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LabLink 文件同步请求。
 */
@Data
public class LabLinkFileSyncRequest {

    @NotBlank(message = "LabLink用户ID不能为空")
    private String labLinkUserId;

    private String username;

    /**
     * LabLink 用户逻辑文件 ID，例如 f_123。
     */
    @NotBlank(message = "LabLink文件ID不能为空")
    private String labLinkFileId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件类型：pdf / doc / docx / txt / md / html。
     */
    private String fileType;

    private Long fileSize;

    private String contentType;

    /**
     * LabLink MinIO objectKey。
     */
    @NotBlank(message = "MinIO objectKey不能为空")
    private String objectKey;

    /**
     * 文件哈希，可选。
     */
    private String sha256;
}