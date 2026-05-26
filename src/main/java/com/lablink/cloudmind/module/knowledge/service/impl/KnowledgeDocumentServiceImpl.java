package com.lablink.cloudmind.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.util.IdParseUtils;
import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.document.model.StoredFile;
import com.lablink.cloudmind.module.document.service.LocalFileStorageService;
import com.lablink.cloudmind.module.knowledge.dto.AddKnowledgeDocumentRequest;
import com.lablink.cloudmind.module.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.DocumentSourceTypeEnum;
import com.lablink.cloudmind.module.knowledge.enums.IngestStatusEnum;
import com.lablink.cloudmind.module.knowledge.enums.ParseStatusEnum;
import com.lablink.cloudmind.module.knowledge.mapper.KnowledgeDocumentMapper;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.knowledge.vo.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    private static final String DEFAULT_PARSER_TYPE = "tika";

    private static final String DEFAULT_EMBEDDING_MODEL = "bge-base-zh-v1.5";

    private static final Integer DEFAULT_EMBEDDING_DIM = 768;

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "doc", "docx", "txt", "md", "html");

    private final KnowledgeBaseService knowledgeBaseService;

    private final LocalFileStorageService localFileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO addDocumentToKnowledgeBase(Long knowledgeBaseId, AddKnowledgeDocumentRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 复用知识库权限校验，避免用户把文件挂到别人的知识库下。
        KnowledgeBase knowledgeBase = knowledgeBaseService.getCurrentUserKnowledgeBase(knowledgeBaseId);

        // 现在还没有把云盘模块接进来。当前先做“知识库文档登记”，让知识库详情页和文档状态先跑通。
        // 后面接入云盘模块后，再完善文件解析逻辑。（FileInfo fileInfo = fileService.getCurrentUserFile(fileId);）
        Long fileId = IdParseUtils.parseLongId(request.getFileId(), "文件ID");
        String fileType = normalizeFileType(request.getFileType());

        validateFileType(fileType);
        checkDocumentNotExists(knowledgeBase.getId(), fileId, userId);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setUserId(userId);
        document.setFileId(fileId);
        document.setFileName(request.getFileName());
        document.setFileType(fileType);
        document.setFileSize(request.getFileSize());
        document.setParserType(DEFAULT_PARSER_TYPE);
        document.setParseStatus(ParseStatusEnum.NOT_PARSED.getCode());
        document.setIngestStatus(IngestStatusEnum.NOT_INDEXED.getCode());
        document.setChunkCount(0);
        document.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
        document.setEmbeddingDim(DEFAULT_EMBEDDING_DIM);
        document.setDeleted(0);
        document.setSourceType(DocumentSourceTypeEnum.UPLOAD.getCode());
        document.setErrorMessage(null);

        boolean saved = this.save(document);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加知识库文档失败");
        }

        // 文档数量是列表高频展示字段，采用冗余计数减少列表查询成本。
        increaseKnowledgeBaseDocumentCount(knowledgeBase.getId());

        KnowledgeDocument savedDocument = this.getById(document.getId());
        return convertToVO(savedDocument);
    }

    @Override
    public PageResult<KnowledgeDocumentVO> pageDocuments(Long knowledgeBaseId, KnowledgeDocumentQueryRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getDeleted())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10L : request.getPageSize();

        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .like(StringUtils.hasText(request.getKeyword()), KnowledgeDocument::getFileName, request.getKeyword())
                .orderByDesc(KnowledgeDocument::getCreateTime);

        Page<KnowledgeDocument> page = this.page(new Page<>(pageNo, pageSize), wrapper);

        List<KnowledgeDocumentVO> records = page.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public KnowledgeDocumentVO getDocumentDetail(Long documentId) {
        KnowledgeDocument document = this.getById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        return convertToVO(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDocument(Long documentId) {
        KnowledgeDocument document = getCurrentUserDocument(documentId);

        boolean removed = this.removeById(document.getId());
        if (!removed) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移除知识库文档失败");
        }

        decreaseKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO uploadDocumentToKnowledgeBase(Long knowledgeBaseId, MultipartFile file) {
        Long userId = UserContext.getCurrentUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.getCurrentUserKnowledgeBase(knowledgeBaseId);
        StoredFile storedFile = localFileStorageService.save(file);

        String fileType = normalizeFileType(storedFile.getFileType());
        validateFileType(fileType);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setUserId(userId);

        // 直接上传场景下暂时生成一个内部 fileId，后续接云盘系统时替换为真实 file_info.id。
        document.setFileId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());

        document.setFileName(storedFile.getOriginalFilename());
        document.setFileType(fileType);
        document.setFileSize(storedFile.getFileSize());
        document.setStorageType(storedFile.getStorageType());
        document.setSourceType(DocumentSourceTypeEnum.UPLOAD.getCode());
        document.setExternalFileId(null);
        document.setExternalFileUrl(null);
        document.setStoragePath(storedFile.getStoragePath());
        document.setContentType(storedFile.getContentType());
        document.setParserType(DEFAULT_PARSER_TYPE);
        document.setParseStatus(ParseStatusEnum.NOT_PARSED.getCode());
        document.setIngestStatus(IngestStatusEnum.NOT_INDEXED.getCode());
        document.setChunkCount(0);
        document.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
        document.setEmbeddingDim(DEFAULT_EMBEDDING_DIM);
        document.setDeleted(0);

        boolean saved = this.save(document);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传知识库文档失败");
        }

        increaseKnowledgeBaseDocumentCount(knowledgeBase.getId());

        return convertToVO(this.getById(document.getId()));
    }

    @Override
    public KnowledgeDocument getCurrentUserDocumentEntity(Long documentId) {
        return getCurrentUserDocument(documentId);
    }

    @Override
    public void markParsing(Long documentId) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getParseStatus, ParseStatusEnum.PARSING.getCode())
                .set(KnowledgeDocument::getErrorMessage, null)
                .update();
    }

    @Override
    public void markParseSuccess(Long documentId, Integer chunkCount, String parserType) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getParseStatus, ParseStatusEnum.SUCCESS.getCode())
                .set(KnowledgeDocument::getChunkCount, chunkCount)
                .set(StringUtils.hasText(parserType), KnowledgeDocument::getParserType, parserType)
                .set(KnowledgeDocument::getErrorMessage, null)
                .update();
    }

    @Override
    public void markParseFailed(Long documentId, String errorMessage) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getParseStatus, ParseStatusEnum.FAILED.getCode())
                .set(KnowledgeDocument::getErrorMessage, errorMessage)
                .update();
    }

    @Override
    public void markIngesting(Long documentId) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getIngestStatus, IngestStatusEnum.PROCESSING.getCode())
                .set(KnowledgeDocument::getErrorMessage, null)
                .update();
    }

    @Override
    public void markIngestSuccess(Long documentId) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getIngestStatus, IngestStatusEnum.SUCCESS.getCode())
                .set(KnowledgeDocument::getErrorMessage, null)
                .update();
    }

    @Override
    public void markIngestFailed(Long documentId, String errorMessage) {
        this.lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .set(KnowledgeDocument::getIngestStatus, IngestStatusEnum.FAILED.getCode())
                .set(KnowledgeDocument::getErrorMessage, errorMessage)
                .update();
    }

    private void checkDocumentNotExists(Long knowledgeBaseId, Long fileId, Long userId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocument::getFileId, fileId)
                .eq(KnowledgeDocument::getUserId, userId);

        long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_ALREADY_EXISTS);
        }
    }

    private KnowledgeDocument getCurrentUserDocument(Long documentId) {
        Long userId = UserContext.getCurrentUserId();

        KnowledgeDocument document = this.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        if (!userId.equals(document.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return document;
    }

    private String normalizeFileType(String fileType) {
        return fileType.trim().toLowerCase();
    }

    private void validateFileType(String fileType) {
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    private void increaseKnowledgeBaseDocumentCount(Long knowledgeBaseId) {
        knowledgeBaseService.lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .setSql("document_count = document_count + 1")
                .update();
    }

    private void decreaseKnowledgeBaseDocumentCount(Long knowledgeBaseId) {
        knowledgeBaseService.lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .setSql("document_count = CASE WHEN document_count > 0 THEN document_count - 1 ELSE 0 END")
                .update();
    }

    private KnowledgeDocumentVO convertToVO(KnowledgeDocument document) {
        if (document == null) {
            return null;
        }

        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setDocumentId(String.valueOf(document.getId()));
        vo.setKnowledgeBaseId(String.valueOf(document.getKnowledgeBaseId()));
        vo.setFileId(String.valueOf(document.getFileId()));
        vo.setFileName(document.getFileName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParserType(document.getParserType());
        vo.setParseStatus(document.getParseStatus());
        vo.setIngestStatus(document.getIngestStatus());
        vo.setChunkCount(document.getChunkCount());
        vo.setEmbeddingModel(document.getEmbeddingModel());
        vo.setEmbeddingDim(document.getEmbeddingDim());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        vo.setStorageType(document.getStorageType());
        vo.setStoragePath(document.getStoragePath());
        vo.setContentType(document.getContentType());
        vo.setSourceType(document.getSourceType());
        vo.setExternalFileId(document.getExternalFileId());
        vo.setExternalFileUrl(document.getExternalFileUrl());
        return vo;
    }
}