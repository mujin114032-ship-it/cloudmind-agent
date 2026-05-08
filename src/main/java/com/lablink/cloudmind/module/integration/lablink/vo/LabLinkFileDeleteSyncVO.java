package com.lablink.cloudmind.module.integration.lablink.vo;

import lombok.Data;

/**
 * LabLink 文件删除同步结果。
 */
@Data
public class LabLinkFileDeleteSyncVO {

    private Boolean deleted;

    private Boolean skipped;

    private String skipReason;

    private String knowledgeBaseId;

    private String documentId;

    private String labLinkFileId;

    private Integer disabledChunkCount;
}