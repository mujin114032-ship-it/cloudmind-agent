package com.lablink.cloudmind.module.integration.lablink.vo;

import lombok.Data;

/**
 * LabLink 文件同步结果。
 */
@Data
public class LabLinkFileSyncVO {

    private Boolean synced;

    private Boolean skipped;

    private String skipReason;

    private String knowledgeBaseId;

    private String knowledgeBaseName;

    private String documentId;

    private String fileName;

    private String sourceType;

    private String storageType;

    private String parseStatus;

    private String ingestStatus;
}