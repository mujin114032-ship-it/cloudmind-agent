package com.lablink.cloudmind.module.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCRmyPDF 解析配置。
 *
 * OCRmyPDF 是外部命令行工具，不通过 Maven 依赖安装。
 * 服务器需要提前安装 ocrmypdf、tesseract 及对应语言包。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudmind.parser.ocrmypdf")
public class OcrMyPdfProperties {

    /**
     * 是否开启 OCRmyPDF 兜底解析。
     */
    private Boolean enabled = false;

    /**
     * OCRmyPDF 命令名或绝对路径，例如：ocrmypdf 或 /usr/bin/ocrmypdf。
     */
    private String command = "ocrmypdf";

    /**
     * OCR 语言，中文论文建议 chi_sim+eng。
     */
    private String language = "chi_sim+eng";

    /**
     * 单个 PDF 的 OCR 超时时间，单位秒。
     */
    private Integer timeoutSeconds = 300;

    /**
     * 最大允许 OCR 的页数，避免超大 PDF 拖垮服务器。
     * 小于等于 0 表示不限制。
     */
    private Integer maxPages = 80;

    /**
     * 是否强制 OCR。
     * 作为 fallback 时建议开启，避免已有劣质文本层导致 OCR 被跳过。
     */
    private Boolean forceOcr = true;

    /**
     * 是否自动校正页面倾斜。
     * 会增加耗时，但对扫描件质量有帮助。
     */
    private Boolean deskew = false;

    /**
     * 已抽取文本低于该长度时，认为 PDF 文本层质量不足，可以继续尝试 OCR。
     */
    private Integer minExtractedTextLength = 30;
}