package com.lablink.cloudmind.module.document.parser;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.document.config.OcrMyPdfProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 OCRmyPDF 的扫描版 PDF 兜底解析器。
 *
 * 实现方式：
 * 调用外部 ocrmypdf 命令，通过 --sidecar 输出 OCR 文本，
 * Java 只读取 sidecar 文本进入后续 chunk / embedding 流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrMyPdfParser {

    private final OcrMyPdfProperties properties;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    /**
     * 判断 Tika / PDFBox 抽取出来的 PDF 文本是否有价值。
     * 如果太短，则认为可能是扫描版 PDF，需要继续 OCR。
     */
    public boolean isExtractedTextUseful(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        int minLength = properties.getMinExtractedTextLength() == null
                ? 30
                : Math.max(0, properties.getMinExtractedTextLength());

        String compactText = text.replaceAll("\\s+", "");
        return compactText.length() >= minLength;
    }

    public String parse(Path inputPdfPath) {
        if (!isEnabled()) {
            return "";
        }

        if (inputPdfPath == null || !Files.exists(inputPdfPath)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSEABLE, "OCR 解析失败：PDF 文件不存在");
        }

        int pageCount = countPages(inputPdfPath);

        Integer maxPages = properties.getMaxPages();
        if (maxPages != null && maxPages > 0 && pageCount > maxPages) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_PARSE_ERROR,
                    "PDF 页数为 " + pageCount + "，超过 OCR 最大页数限制 " + maxPages
            );
        }

        Path workDir = null;

        try {
            workDir = Files.createTempDirectory("cloudmind-ocrmypdf-");

            Path outputPdfPath = workDir.resolve("ocr-output.pdf");
            Path sidecarTextPath = workDir.resolve("ocr-output.txt");

            List<String> command = buildCommand(inputPdfPath, outputPdfPath, sidecarTextPath);

            log.info("开始 OCRmyPDF 解析：input={}, pages={}, command={}",
                    inputPdfPath, pageCount, command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            Thread outputReader = new Thread(
                    () -> copyProcessOutput(process.getInputStream(), outputBuffer),
                    "ocrmypdf-output-reader"
            );
            outputReader.setDaemon(true);
            outputReader.start();

            long timeout = normalizeTimeoutSeconds();
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(
                        ErrorCode.DOCUMENT_PARSE_ERROR,
                        "OCRmyPDF 解析超时，超过 " + timeout + " 秒"
                );
            }

            outputReader.join(Duration.ofSeconds(5).toMillis());

            String commandOutput = outputBuffer.toString(StandardCharsets.UTF_8);
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.warn("OCRmyPDF 执行失败：exitCode={}, output={}", exitCode, commandOutput);
                throw new BusinessException(
                        ErrorCode.DOCUMENT_PARSE_ERROR,
                        "OCRmyPDF 执行失败，exitCode=" + exitCode
                                + "，请确认 ocrmypdf / tesseract / 中文语言包已安装"
                );
            }

            if (!Files.exists(sidecarTextPath)) {
                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "OCRmyPDF 未生成 sidecar 文本文件");
            }

            String text = Files.readString(sidecarTextPath, StandardCharsets.UTF_8);

            if (!StringUtils.hasText(text)) {
                throw new BusinessException(ErrorCode.DOCUMENT_CHUNK_EMPTY, "OCRmyPDF 解析结果为空");
            }

            log.info("OCRmyPDF 解析完成：input={}, pages={}, textLength={}",
                    inputPdfPath, pageCount, text.length());

            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OCRmyPDF 解析异常：input={}, reason={}", inputPdfPath, ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "OCRmyPDF 解析异常：" + ex.getMessage());
        } finally {
            deleteDirectoryQuietly(workDir);
        }
    }

    private List<String> buildCommand(Path inputPdfPath, Path outputPdfPath, Path sidecarTextPath) {
        List<String> command = new ArrayList<>();

        command.add(properties.getCommand());
        command.add("--sidecar");
        command.add(sidecarTextPath.toString());
        command.add("-l");
        command.add(StringUtils.hasText(properties.getLanguage()) ? properties.getLanguage() : "chi_sim+eng");
        command.add("--optimize");
        command.add("0");

        if (Boolean.TRUE.equals(properties.getForceOcr())) {
            command.add("--force-ocr");
        } else {
            command.add("--skip-text");
        }

        if (Boolean.TRUE.equals(properties.getDeskew())) {
            command.add("--deskew");
        }

        command.add(inputPdfPath.toString());
        command.add(outputPdfPath.toString());

        return command;
    }

    private int countPages(Path inputPdfPath) {
        try (PDDocument document = Loader.loadPDF(inputPdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "PDF 文件已加密，暂不支持 OCR 解析");
            }
            return document.getNumberOfPages();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR, "读取 PDF 页数失败：" + ex.getMessage());
        }
    }

    private long normalizeTimeoutSeconds() {
        Integer configured = properties.getTimeoutSeconds();
        return configured == null || configured <= 0 ? 300L : configured.longValue();
    }

    private void copyProcessOutput(InputStream inputStream, ByteArrayOutputStream outputBuffer) {
        try (inputStream) {
            inputStream.transferTo(outputBuffer);
        } catch (IOException ex) {
            log.warn("读取 OCRmyPDF 输出失败：{}", ex.getMessage());
        }
    }

    private void deleteDirectoryQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }

        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ex) {
            log.warn("删除 OCR 临时目录失败：dir={}, reason={}", dir, ex.getMessage());
        }
    }
}