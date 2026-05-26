package com.lablink.cloudmind.module.knowledge.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.document.chunker.SlidingWindowTextChunker;
import com.lablink.cloudmind.module.document.parser.OcrMyPdfParser;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    private final OcrMyPdfParser ocrMyPdfParser;

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

            ParsedText parsedText = parseDocumentText(document);
            String text = parsedText.text();
            String parserType = parsedText.parserType();

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
            knowledgeDocumentService.markParseSuccess(documentId, chunks.size(), parserType);

            log.info("文档解析完成：documentId={}, parserType={}, chunkCount={}",
                    documentId, parserType, chunks.size());
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

    private ParsedText parseDocumentText(KnowledgeDocument document) throws Exception {
        if (isPdf(document)) {
            return parsePdfDocument(document);
        }

        try (InputStream inputStream = documentContentLoaderManager.load(document)) {
            String text = tikaDocumentParser.parse(
                    inputStream,
                    document.getFileName(),
                    document.getContentType()
            );
            return new ParsedText(text, "tika");
        }
    }

    /**
     * PDF 统一解析链路。
     *
     * 无论文件来自本地上传还是 LabLink MinIO，
     * 都先同步为临时文件，再统一走：
     *
     * Tika → PDFBox → OCRmyPDF
     */
    private ParsedText parsePdfDocument(KnowledgeDocument document) throws Exception {
        Path tempPdfPath = Files.createTempFile("cloudmind-pdf-", ".pdf");

        try {
            try (InputStream inputStream = documentContentLoaderManager.load(document)) {
                Files.copy(inputStream, tempPdfPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String text = "";

            try (InputStream tikaInputStream = Files.newInputStream(tempPdfPath)) {
                text = tikaDocumentParser.parse(
                        tikaInputStream,
                        document.getFileName(),
                        document.getContentType()
                );
            } catch (BusinessException ex) {
                log.warn("PDF Tika 解析失败，继续尝试 PDFBox：documentId={}, reason={}",
                        document.getId(), ex.getMessage());
            }

            if (isUsefulPdfText(text)) {
                return new ParsedText(text, "tika");
            }

            try {
                text = pdfTextParser.parse(tempPdfPath);
            } catch (BusinessException ex) {
                log.warn("PDFBox 解析失败，继续尝试 OCRmyPDF：documentId={}, reason={}",
                        document.getId(), ex.getMessage());
            }

            if (isUsefulPdfText(text)) {
                return new ParsedText(text, "pdfbox");
            }

            if (ocrMyPdfParser.isEnabled()) {
                text = ocrMyPdfParser.parse(tempPdfPath);
                return new ParsedText(text, "ocrmypdf");
            }

            return new ParsedText(text, "pdfbox");
        } finally {
            deleteTempFileQuietly(tempPdfPath);
        }
    }

    private boolean isUsefulPdfText(String text) {
        if (!ocrMyPdfParser.isEnabled()) {
            return StringUtils.hasText(text);
        }
        return ocrMyPdfParser.isExtractedTextUseful(text);
    }

    private void deleteTempFileQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            log.warn("删除 PDF 解析临时文件失败：path={}, reason={}", path, ex.getMessage());
        }
    }

    private boolean isPdf(KnowledgeDocument document) {
        return "pdf".equalsIgnoreCase(document.getFileType());
    }

    private record ParsedText(String text, String parserType) {
    }
}