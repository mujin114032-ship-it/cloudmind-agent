package com.lablink.cloudmind.module.knowledge.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.document.chunker.SlidingWindowTextChunker;
import com.lablink.cloudmind.module.document.parser.PdfTextParser;
import com.lablink.cloudmind.module.document.parser.TikaDocumentParser;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * 知识库文档解析应用服务。
 *
 * <p>负责串联：文档状态更新 → 文档解析 → 文本分块 → chunk 落库。</p>
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentParseApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final TikaDocumentParser tikaDocumentParser;

    private final PdfTextParser pdfTextParser;

    private final SlidingWindowTextChunker textChunker;

    private final DocumentChunkService documentChunkService;

    public void parseAndChunk(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getCurrentUserDocumentEntity(documentId);

        if (!StringUtils.hasText(document.getStoragePath())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSEABLE);
        }

        knowledgeDocumentService.markParsing(documentId);

        try {
            Path filePath = Path.of(document.getStoragePath());

            String text = tikaDocumentParser.parse(filePath);

            // PDF 场景下，Tika 解析为空时使用 PDFBox 再尝试一次。
            if (!StringUtils.hasText(text) && isPdf(document)) {
                text = pdfTextParser.parse(filePath);
            }

            if (!StringUtils.hasText(text)) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_CHUNK_EMPTY,
                        "文档解析结果为空，可能是扫描版 PDF、图片型 PDF 或文件没有可抽取文本层"
                );
            }

            List<String> chunks = textChunker.chunk(text);

            documentChunkService.replaceDocumentChunks(document, chunks);
            knowledgeDocumentService.markParseSuccess(documentId, chunks.size());
        } catch (Exception ex) {
            knowledgeDocumentService.markParseFailed(documentId, ex.getMessage());
            throw ex;
        }
    }

    private boolean isPdf(KnowledgeDocument document) {
        return "pdf".equalsIgnoreCase(document.getFileType());
    }
}