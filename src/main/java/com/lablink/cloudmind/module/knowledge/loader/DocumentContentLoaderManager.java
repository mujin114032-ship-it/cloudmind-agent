package com.lablink.cloudmind.module.knowledge.loader;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 文档内容加载器管理器。
 *
 * <p>根据文档来源选择合适的 Loader。</p>
 */
@Component
@RequiredArgsConstructor
public class DocumentContentLoaderManager {

    private final List<DocumentContentLoader> loaders;

    public InputStream load(KnowledgeDocument document) {
        DocumentContentLoader loader = loaders.stream()
                .filter(item -> item.supports(document))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DOCUMENT_SOURCE_NOT_SUPPORTED,
                        "不支持的文档来源：sourceType=" + document.getSourceType()
                                + ", storageType=" + document.getStorageType()
                ));

        return loader.load(document);
    }

    public String resolveLoaderName(KnowledgeDocument document) {
        return loaders.stream()
                .filter(item -> item.supports(document))
                .map(DocumentContentLoader::loaderName)
                .findFirst()
                .orElse("unknown");
    }
}