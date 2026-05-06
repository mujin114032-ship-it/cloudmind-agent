package com.lablink.cloudmind.module.knowledge.parser.impl;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.knowledge.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 基于 Apache Tika 的文档解析器。
 */
@Slf4j
@Component
public class TikaDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();

    @Override
    public String parse(InputStream inputStream, String fileName, String contentType) {
        try {
            Metadata metadata = new Metadata();

            if (StringUtils.hasText(fileName)) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }

            if (StringUtils.hasText(contentType)) {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }

            return tika.parseToString(inputStream, metadata);
        } catch (Exception ex) {
            log.warn("Tika解析文档失败：fileName={}, contentType={}, reason={}",
                    fileName,
                    contentType,
                    ex.getMessage(),
                    ex);

            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "文档解析失败：" + ex.getMessage());
        }
    }
}