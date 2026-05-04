package com.lablink.cloudmind.module.document.chunker;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口文本分块器。
 *
 * <p>第一阶段按字符长度分块，后续可升级为按 token 或语义分块。</p>
 */
@Component
public class SlidingWindowTextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 600;

    private static final int DEFAULT_OVERLAP = 100;

    public List<String> chunk(String rawText) {
        String text = normalize(rawText);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.DOCUMENT_CHUNK_EMPTY);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + DEFAULT_CHUNK_SIZE, length);
            String chunk = text.substring(start, end).trim();

            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }

            if (end == length) {
                break;
            }

            start = Math.max(0, end - DEFAULT_OVERLAP);
        }

        return chunks;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}