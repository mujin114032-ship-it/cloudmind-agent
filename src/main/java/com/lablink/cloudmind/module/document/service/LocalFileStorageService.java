package com.lablink.cloudmind.module.document.service;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.document.model.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地文件存储服务。
 *
 * <p>仅用于第一阶段快速跑通文档上传与解析，后续可替换为 MinIO/OSS。</p>
 */
@Service
public class LocalFileStorageService {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    @Value("${cloudmind.storage.local-root:./data/uploads}")
    private String localRoot;

    public StoredFile save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能超过100MB");
        }

        try {
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileType = getFileType(originalFilename);
            String storedFilename = UUID.randomUUID() + "." + fileType;

            Path dir = Path.of(localRoot, LocalDate.now().toString());
            Files.createDirectories(dir);

            Path targetPath = dir.resolve(storedFilename).toAbsolutePath();
            file.transferTo(targetPath);

            return new StoredFile(
                    "local",
                    targetPath.toString(),
                    originalFilename,
                    fileType,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "文件保存失败：" + ex.getMessage());
        }
    }

    private String getFileType(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "无法识别文件类型");
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}