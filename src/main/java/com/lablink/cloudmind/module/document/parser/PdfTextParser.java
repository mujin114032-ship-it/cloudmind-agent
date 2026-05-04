package com.lablink.cloudmind.module.document.parser;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF 文本兜底解析器。
 *
 * <p>用于在 Tika 自动解析为空时，直接使用 PDFBox 尝试抽取 PDF 文本层。</p>
 */
@Component
public class PdfTextParser {

    public String parse(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSEABLE);
        }

        File file = filePath.toFile();

        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "PDF 文件已加密，暂不支持解析");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            return StringUtils.hasText(text) ? text : "";
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "PDFBox 解析 PDF 失败：" + ex.getMessage());
        }
    }
}