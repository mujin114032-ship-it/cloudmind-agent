package com.lablink.cloudmind.module.knowledge.loader.impl;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.integration.lablink.config.LabLinkIntegrationProperties;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.DocumentSourceTypeEnum;
import com.lablink.cloudmind.module.knowledge.loader.DocumentContentLoader;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * LabLink MinIO 文件加载器。
 *
 * <p>用于读取从 LabLink 同步过来的文件。CloudMind 不复制文件，只记录 LabLink 的 MinIO objectKey。</p>
 */
@Slf4j
@Component
public class LabLinkMinioDocumentContentLoader implements DocumentContentLoader {

    private static final String STORAGE_TYPE_MINIO = "minio";

    private final MinioClient labLinkMinioClient;

    private final LabLinkIntegrationProperties properties;

    public LabLinkMinioDocumentContentLoader(
            @Qualifier("labLinkMinioClient") MinioClient labLinkMinioClient,
            LabLinkIntegrationProperties properties
    ) {
        this.labLinkMinioClient = labLinkMinioClient;
        this.properties = properties;
    }

    @Override
    public boolean supports(KnowledgeDocument document) {
        if (document == null) {
            return false;
        }

        return DocumentSourceTypeEnum.LABLINK_FILE.getCode().equals(document.getSourceType())
                && STORAGE_TYPE_MINIO.equals(document.getStorageType())
                && StringUtils.hasText(document.getStoragePath());
    }

    @Override
    public InputStream load(KnowledgeDocument document) {
        String bucketName = properties.getMinio().getBucketName();
        String objectKey = document.getStoragePath();

        if (!StringUtils.hasText(bucketName)) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "LabLink MinIO bucket 未配置");
        }

        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "LabLink 文件 objectKey 为空");
        }

        try {
            log.info("读取 LabLink MinIO 文件：documentId={}, bucket={}, objectKey={}",
                    document.getId(),
                    bucketName,
                    objectKey);

            return labLinkMinioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            log.warn("读取 LabLink MinIO 文件失败：documentId={}, objectKey={}, reason={}",
                    document.getId(),
                    objectKey,
                    ex.getMessage(),
                    ex);

            throw new BusinessException(
                    ErrorCode.DOCUMENT_LOAD_ERROR,
                    "读取 LabLink MinIO 文件失败：" + ex.getMessage()
            );
        }
    }

    @Override
    public String loaderName() {
        return "lablink-minio";
    }
}