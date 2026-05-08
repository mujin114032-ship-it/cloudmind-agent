package com.lablink.cloudmind.module.integration.lablink.support;

import com.lablink.cloudmind.module.integration.lablink.config.LabLinkIntegrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Agent 知识库支持文件类型判断。
 */
@Component
@RequiredArgsConstructor
public class AgentDocumentTypeSupport {

    private final LabLinkIntegrationProperties properties;

    public boolean isSupported(String fileType, String fileName) {
        String normalized = normalizeFileType(fileType, fileName);

        if (!StringUtils.hasText(normalized)) {
            return false;
        }

        return properties.getSupportedFileTypes()
                .stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> item.equals(normalized));
    }

    public String normalizeFileType(String fileType, String fileName) {
        if (StringUtils.hasText(fileType)) {
            return fileType.trim().toLowerCase(Locale.ROOT);
        }

        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT);
        }

        return "";
    }
}