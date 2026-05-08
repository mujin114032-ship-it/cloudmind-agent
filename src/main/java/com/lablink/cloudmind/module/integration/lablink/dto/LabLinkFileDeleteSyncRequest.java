package com.lablink.cloudmind.module.integration.lablink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LabLink 文件删除同步请求。
 *
 * <p>当前阶段只做逻辑删除，不物理删除 MinIO 文件和 Milvus 向量。</p>
 */
@Data
public class LabLinkFileDeleteSyncRequest {

    @NotBlank(message = "LabLink用户ID不能为空")
    private String labLinkUserId;

    private String username;

    /**
     * LabLink 用户逻辑文件 ID，例如 f_6。
     */
    @NotBlank(message = "LabLink文件ID不能为空")
    private String labLinkFileId;

    /**
     * 删除原因，例如 lablink_delete。
     */
    private String deleteReason;
}