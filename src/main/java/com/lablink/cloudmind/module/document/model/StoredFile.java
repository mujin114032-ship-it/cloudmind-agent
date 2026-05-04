package com.lablink.cloudmind.module.document.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件保存后的结果。
 *
 * <p>第一阶段使用本地文件路径，后续可以替换为 MinIO/OSS 的 objectKey。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private String storageType;

    private String storagePath;

    private String originalFilename;

    private String fileType;

    private Long fileSize;

    private String contentType;
}