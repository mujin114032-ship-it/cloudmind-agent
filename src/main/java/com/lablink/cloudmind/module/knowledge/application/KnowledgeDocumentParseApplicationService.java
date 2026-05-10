package com.lablink.cloudmind.module.knowledge.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.document.chunker.SlidingWindowTextChunker;
import com.lablink.cloudmind.module.document.parser.PdfTextParser;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.loader.DocumentContentLoaderManager;
import com.lablink.cloudmind.module.knowledge.parser.impl.TikaDocumentParser;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 知识库文档解析应用服务。
 *
 * <p>负责串联：文档读取 → 文档解析 → 文本分块 → chunk 落库 → 状态更新。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentParseApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentContentLoaderManager documentContentLoaderManager;

    private final TikaDocumentParser tikaDocumentParser;

    private final PdfTextParser pdfTextParser;

    private final SlidingWindowTextChunker textChunker;

    private final DocumentChunkService documentChunkService;

    public void parseAndChunk(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getCurrentUserDocumentEntity(documentId);
        doParseAndChunk(document);
    }

    /**
     * 内部解析入口。
     *
     * <p>用于 LabLink 文件同步后的自动解析，不依赖 UserContext。</p>
     */
    public void parseAndChunkInternal(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        doParseAndChunk(document);
    }

    private void doParseAndChunk(KnowledgeDocument document) {
        Long documentId = document.getId();

        knowledgeDocumentService.markParsing(documentId);

        try {
            String loaderName = documentContentLoaderManager.resolveLoaderName(document);

            log.info("开始解析文档：documentId={}, fileName={}, sourceType={}, storageType={}, loader={}",
                    document.getId(),
                    document.getFileName(),
                    document.getSourceType(),
                    document.getStorageType(),
                    loaderName);

            String text;

            // 第一优先级：通过 DocumentContentLoader 读取文件流，再交给 Tika 解析。
            try (InputStream inputStream = documentContentLoaderManager.load(document)) {
                text = tikaDocumentParser.parse(
                        inputStream,
                        document.getFileName(),
                        document.getContentType()
                );
            }

            /*
             * 兼容旧逻辑：
             * 只有 local 文档才用 PdfTextParser fallback。
             * LabLink MinIO 文档的 storagePath 是 objectKey，不是本地路径，不能 Paths.get 后当本地文件解析。
             */
            if (!StringUtils.hasText(text)
                    && isPdf(document)
                    && StringUtils.hasText(document.getStoragePath())
                    && ("local".equalsIgnoreCase(document.getStorageType())
                    || !StringUtils.hasText(document.getStorageType()))) {
                Path filePath = Paths.get(document.getStoragePath());
                text = pdfTextParser.parse(filePath);
            }

            if (!StringUtils.hasText(text)) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_CHUNK_EMPTY,
                        "文档解析结果为空，可能是扫描版 PDF、图片型 PDF 或文件没有可抽取文本层"
                );
            }

            List<String> chunks = textChunker.chunk(text);

            if (chunks == null || chunks.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_CHUNK_EMPTY,
                        "文档分块结果为空"
                );
            }

            documentChunkService.replaceDocumentChunks(document, chunks);
            knowledgeDocumentService.markParseSuccess(documentId, chunks.size());

            log.info("文档解析完成：documentId={}, chunkCount={}", documentId, chunks.size());
        } catch (BusinessException ex) {
            knowledgeDocumentService.markParseFailed(documentId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("文档解析失败：documentId={}, reason={}", documentId, ex.getMessage(), ex);
            knowledgeDocumentService.markParseFailed(documentId, ex.getMessage());
            throw new BusinessException(
                    ErrorCode.DOCUMENT_PARSE_ERROR,
                    "文档解析失败：" + ex.getMessage()
            );
        }
    }

    private boolean isPdf(KnowledgeDocument document) {
        return "pdf".equalsIgnoreCase(document.getFileType());
    }
}