package com.lablink.cloudmind.module.integration.lablink.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkFileDeleteSyncRequest;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkFileSyncRequest;
import com.lablink.cloudmind.module.integration.lablink.support.AgentDocumentTypeSupport;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkFileDeleteSyncVO;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkFileSyncVO;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.DocumentSourceTypeEnum;
import com.lablink.cloudmind.module.knowledge.enums.IngestStatusEnum;
import com.lablink.cloudmind.module.knowledge.enums.ParseStatusEnum;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * LabLink 文件同步应用服务。
 */
@Service
@RequiredArgsConstructor
public class LabLinkFileSyncApplicationService {

    private static final String STORAGE_TYPE_MINIO = "minio";

    private static final String DEFAULT_PARSER_TYPE = "tika";

    private static final String DEFAULT_EMBEDDING_MODEL = "bge-base-zh-v1.5";

    private static final Integer DEFAULT_EMBEDDING_DIM = 768;

    private final KnowledgeBaseService knowledgeBaseService;

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final AgentDocumentTypeSupport agentDocumentTypeSupport;

    private final LabLinkDocumentAutoIngestService labLinkDocumentAutoIngestService;

    @Transactional(rollbackFor = Exception.class)
    public LabLinkFileSyncVO syncFile(LabLinkFileSyncRequest request) {
        Long labLinkUserId = parseLabLinkUserId(request.getLabLinkUserId());

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                labLinkUserId,
                request.getUsername()
        );

        String normalizedFileType = agentDocumentTypeSupport.normalizeFileType(
                request.getFileType(),
                request.getFileName()
        );

        if (!agentDocumentTypeSupport.isSupported(normalizedFileType, request.getFileName())) {
            return skipped(
                    knowledgeBase,
                    request,
                    "unsupported_file_type"
            );
        }

        KnowledgeDocument existing = knowledgeDocumentService.lambdaQuery()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBase.getId())
                .eq(KnowledgeDocument::getUserId, labLinkUserId)
                .eq(KnowledgeDocument::getSourceType, DocumentSourceTypeEnum.LABLINK_FILE.getCode())
                .eq(KnowledgeDocument::getExternalFileId, request.getLabLinkFileId())
                .one();

        if (existing != null) {
            boolean submitted = submitAutoIngestAfterCommitIfNecessary(existing);
            return existed(knowledgeBase, existing, submitted);
        }

        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setUserId(labLinkUserId);
        document.setFileId(IdWorker.getId());

        document.setFileName(request.getFileName());
        document.setFileType(normalizedFileType);
        document.setFileSize(request.getFileSize() == null ? 0L : request.getFileSize());

        document.setParserType(DEFAULT_PARSER_TYPE);
        document.setParseStatus(ParseStatusEnum.NOT_PARSED.getCode());
        document.setIngestStatus(IngestStatusEnum.NOT_INDEXED.getCode());
        document.setChunkCount(0);

        document.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
        document.setEmbeddingDim(DEFAULT_EMBEDDING_DIM);

        document.setStorageType(STORAGE_TYPE_MINIO);
        document.setStoragePath(request.getObjectKey());
        document.setContentType(request.getContentType());

        document.setSourceType(DocumentSourceTypeEnum.LABLINK_FILE.getCode());
        document.setExternalFileId(request.getLabLinkFileId());
        document.setExternalFileUrl(null);

        document.setErrorMessage(null);
        document.setDeleted(0);

        boolean saved = knowledgeDocumentService.save(document);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "同步 LabLink 文件失败");
        }

        knowledgeBaseService.lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBase.getId())
                .setSql("document_count = document_count + 1")
                .update();

        submitAutoIngestAfterCommit(document.getId());

        return synced(knowledgeBase, document, true);
    }

    private LabLinkFileSyncVO synced(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            boolean autoIngestSubmitted
    ) {
        LabLinkFileSyncVO vo = new LabLinkFileSyncVO();
        vo.setSynced(true);
        vo.setSkipped(false);
        vo.setSkipReason(null);
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBase.getId()));
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setDocumentId(String.valueOf(document.getId()));
        vo.setFileName(document.getFileName());
        vo.setSourceType(document.getSourceType());
        vo.setStorageType(document.getStorageType());
        vo.setParseStatus(String.valueOf(document.getParseStatus()));
        vo.setIngestStatus(String.valueOf(document.getIngestStatus()));
        vo.setAutoIngestSubmitted(autoIngestSubmitted);
        return vo;
    }

    private LabLinkFileSyncVO existed(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            boolean autoIngestSubmitted
    ) {
        LabLinkFileSyncVO vo = synced(knowledgeBase, document, autoIngestSubmitted);
        vo.setSynced(false);
        vo.setSkipped(true);
        vo.setSkipReason("already_synced");
        return vo;
    }

    private LabLinkFileSyncVO skipped(
            KnowledgeBase knowledgeBase,
            LabLinkFileSyncRequest request,
            String reason
    ) {
        LabLinkFileSyncVO vo = new LabLinkFileSyncVO();
        vo.setSynced(false);
        vo.setSkipped(true);
        vo.setSkipReason(reason);
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBase.getId()));
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setDocumentId(null);
        vo.setFileName(request.getFileName());
        vo.setSourceType(DocumentSourceTypeEnum.LABLINK_FILE.getCode());
        vo.setStorageType(STORAGE_TYPE_MINIO);
        vo.setAutoIngestSubmitted(false);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public LabLinkFileDeleteSyncVO deleteSync(LabLinkFileDeleteSyncRequest request) {
        Long labLinkUserId = parseLabLinkUserId(request.getLabLinkUserId());

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                labLinkUserId,
                request.getUsername()
        );

        KnowledgeDocument document = knowledgeDocumentService.lambdaQuery()
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBase.getId())
                .eq(KnowledgeDocument::getUserId, labLinkUserId)
                .eq(KnowledgeDocument::getSourceType, DocumentSourceTypeEnum.LABLINK_FILE.getCode())
                .eq(KnowledgeDocument::getExternalFileId, request.getLabLinkFileId())
                .one();

        if (document == null) {
            LabLinkFileDeleteSyncVO vo = new LabLinkFileDeleteSyncVO();
            vo.setDeleted(false);
            vo.setSkipped(true);
            vo.setSkipReason("document_not_found");
            vo.setKnowledgeBaseId(String.valueOf(knowledgeBase.getId()));
            vo.setDocumentId(null);
            vo.setLabLinkFileId(request.getLabLinkFileId());
            vo.setDisabledChunkCount(0);
            return vo;
        }

        Integer chunkCount = document.getChunkCount() == null ? 0 : document.getChunkCount();

        // 1. 禁用并逻辑删除 chunk，保证后续 RAG 不再使用。
        boolean chunkUpdated = documentChunkService.lambdaUpdate()
                .eq(DocumentChunk::getDocumentId, document.getId())
                .set(DocumentChunk::getEnabled, 0)
                .set(DocumentChunk::getDeleted, 1)
                .update();

        // 2. 逻辑删除 document。
        boolean documentRemoved = knowledgeDocumentService.removeById(document.getId());
        if (!documentRemoved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除 LabLink 同步文档失败");
        }

        // 3. 更新知识库统计。Milvus 向量暂时不物理删除。
        knowledgeBaseService.lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBase.getId())
                .setSql("document_count = GREATEST(document_count - 1, 0)")
                .setSql("chunk_count = GREATEST(chunk_count - " + chunkCount + ", 0)")
                .update();

        LabLinkFileDeleteSyncVO vo = new LabLinkFileDeleteSyncVO();
        vo.setDeleted(true);
        vo.setSkipped(false);
        vo.setSkipReason(null);
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBase.getId()));
        vo.setDocumentId(String.valueOf(document.getId()));
        vo.setLabLinkFileId(request.getLabLinkFileId());
        vo.setDisabledChunkCount(chunkCount);
        return vo;
    }

    private Long parseLabLinkUserId(String labLinkUserId) {
        if (!StringUtils.hasText(labLinkUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "LabLink用户ID不能为空");
        }

        try {
            return Long.valueOf(labLinkUserId);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "LabLink用户ID格式错误");
        }
    }

//    private boolean submitAutoIngestAfterCommitIfNecessary(KnowledgeDocument document) {
//        boolean needParse = document.getParseStatus() == null
//                || ParseStatusEnum.NOT_PARSED.getCode().equals(document.getParseStatus());
//
//        boolean needIngest = document.getIngestStatus() == null
//                || IngestStatusEnum.NOT_INDEXED.getCode().equals(document.getIngestStatus());
//
//        if (!needParse && !needIngest) {
//            return false;
//        }
//
//        submitAutoIngestAfterCommit(document.getId());
//        return true;
//    }
    // 只要不是 解析成功 + 入库成功，就补偿触发。
    private boolean submitAutoIngestAfterCommitIfNecessary(KnowledgeDocument document) {
        boolean needParse = !ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus());
        boolean needIngest = !IngestStatusEnum.SUCCESS.getCode().equals(document.getIngestStatus());

        if (!needParse && !needIngest) {
            return false;
        }

        submitAutoIngestAfterCommit(document.getId());
        return true;
    }

    private void submitAutoIngestAfterCommit(Long documentId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    labLinkDocumentAutoIngestService.parseAndIngestAsync(documentId);
                }
            });
            return;
        }

        labLinkDocumentAutoIngestService.parseAndIngestAsync(documentId);
    }
}