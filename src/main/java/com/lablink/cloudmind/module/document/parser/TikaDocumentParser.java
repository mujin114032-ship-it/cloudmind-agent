package com.lablink.cloudmind.module.document.parser;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于 Apache Tika 的通用文档解析器。
 *
 * <p>第一阶段只抽取纯文本内容，不处理表格结构、图片 OCR 等复杂场景。</p>
 */
@Slf4j
@Component
public class TikaDocumentParser {

    public String parse(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSEABLE);
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            AutoDetectParser parser = new AutoDetectParser();

            // -1 表示不限制抽取文本长度，避免长文档被截断。
            BodyContentHandler handler = new BodyContentHandler(-1);

            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString();

            log.info("Tika 解析完成：filePath={}, fileSize={} bytes, contentType={}, textLength={}",
                    filePath,
                    Files.size(filePath),
                    metadata.get(Metadata.CONTENT_TYPE),
                    text == null ? 0 : text.length());

            return StringUtils.hasText(text) ? text : "";
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "文档解析失败：" + ex.getMessage());
        }
    }
}