package com.lablink.cloudmind.module.knowledge.loader.impl;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.DocumentSourceTypeEnum;
import com.lablink.cloudmind.module.knowledge.loader.DocumentContentLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地上传文件加载器。
 */
@Slf4j
@Component
public class LocalDocumentContentLoader implements DocumentContentLoader {

    private static final String STORAGE_TYPE_LOCAL = "local";

    @Override
    public boolean supports(KnowledgeDocument document) {
        if (document == null) {
            return false;
        }

        boolean sourceMatched = !StringUtils.hasText(document.getSourceType())
                || DocumentSourceTypeEnum.UPLOAD.getCode().equals(document.getSourceType());

        boolean storageMatched = !StringUtils.hasText(document.getStorageType())
                || STORAGE_TYPE_LOCAL.equals(document.getStorageType());

        return sourceMatched && storageMatched;
    }

    @Override
    public InputStream load(KnowledgeDocument document) {
        if (!StringUtils.hasText(document.getStoragePath())) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "文档本地路径为空");
        }

        try {
            Path path = Paths.get(document.getStoragePath()).normalize();

            if (!Files.exists(path)) {
                throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "本地文件不存在：" + path);
            }

            if (!Files.isRegularFile(path)) {
                throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "路径不是有效文件：" + path);
            }

            return Files.newInputStream(path);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("读取本地文档失败：documentId={}, path={}, reason={}",
                    document.getId(),
                    document.getStoragePath(),
                    ex.getMessage(),
                    ex);

            throw new BusinessException(ErrorCode.DOCUMENT_LOAD_ERROR, "读取本地文档失败：" + ex.getMessage());
        }
    }

    @Override
    public String loaderName() {
        return "local";
    }
}