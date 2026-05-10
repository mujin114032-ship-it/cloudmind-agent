package com.lablink.cloudmind.module.embedding.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.embedding.client.EmbeddingClient;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestTaskVO;
import com.lablink.cloudmind.module.embedding.entity.DocumentIngestTask;
import com.lablink.cloudmind.module.embedding.enums.DocumentIngestTaskStageEnum;
import com.lablink.cloudmind.module.embedding.service.DocumentIngestTaskService;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.IngestStatusEnum;
import com.lablink.cloudmind.module.knowledge.enums.ParseStatusEnum;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.vector.model.ChunkVectorRow;
import com.lablink.cloudmind.module.vector.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 文档异步向量入库应用服务。
 *
 * <p>请求线程只负责校验和创建任务，真正的向量化与 Milvus 写入在异步线程执行。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAsyncIngestApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final DocumentIngestTaskService documentIngestTaskService;

    private final EmbeddingClient embeddingClient;

    private final VectorStoreService vectorStoreService;

    @Qualifier("documentIngestTaskExecutor")
    private final Executor documentIngestTaskExecutor;

    public DocumentIngestTaskVO submitIngestTask(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getCurrentUserDocumentEntity(documentId);

        if (!ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }

        if (IngestStatusEnum.SUCCESS.getCode().equals(document.getIngestStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_ALREADY_INGESTED);
        }

        DocumentIngestTask runningTask = documentIngestTaskService.getRunningTask(
                document.getId(),
                document.getUserId()
        );

        if (runningTask != null) {
            return documentIngestTaskService.getTaskDetail(runningTask.getId());
        }

        DocumentIngestTask task = documentIngestTaskService.createTask(document);

        documentIngestTaskExecutor.execute(() -> executeTask(task.getId()));

        return documentIngestTaskService.getTaskDetail(task.getId());
    }

    private void executeTask(Long taskId) {
        DocumentIngestTask task = documentIngestTaskService.getById(taskId);
        if (task == null) {
            return;
        }

        Long documentId = task.getDocumentId();

        try {
            documentIngestTaskService.markRunning(
                    taskId,
                    DocumentIngestTaskStageEnum.VALIDATING.getCode(),
                    5
            );

            KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
            if (document == null) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
            }

            if (!task.getUserId().equals(document.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            if (!ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus())) {
                throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
            }

            List<DocumentChunk> chunks = documentChunkService.listEnabledChunksByDocumentId(documentId);
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.DOCUMENT_CHUNK_NOT_FOUND);
            }

            knowledgeDocumentService.markIngesting(documentId);

            documentIngestTaskService.updateProgress(
                    taskId,
                    DocumentIngestTaskStageEnum.EMBEDDING.getCode(),
                    20,
                    chunks.size(),
                    0
            );

            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getChunkText)
                    .toList();

            List<float[]> vectors = embeddingClient.embedDocuments(texts);
            validateVectors(vectors, chunks.size());

            documentIngestTaskService.updateProgress(
                    taskId,
                    DocumentIngestTaskStageEnum.WRITING_VECTOR.getCode(),
                    70,
                    chunks.size(),
                    chunks.size()
            );

            List<ChunkVectorRow> rows = buildVectorRows(chunks, vectors);

            vectorStoreService.insertChunkVectors(rows);

            documentIngestTaskService.updateProgress(
                    taskId,
                    DocumentIngestTaskStageEnum.UPDATING_STATUS.getCode(),
                    90,
                    chunks.size(),
                    chunks.size()
            );

            documentChunkService.markChunksVectorized(chunks);
            knowledgeDocumentService.markIngestSuccess(documentId);

            documentIngestTaskService.markSuccess(
                    taskId,
                    chunks.size(),
                    embeddingClient.modelName(),
                    embeddingClient.dimension()
            );

            log.info("文档异步入库成功：taskId={}, documentId={}, chunkCount={}",
                    taskId,
                    documentId,
                    chunks.size());
        } catch (Exception ex) {
            log.warn("文档异步入库失败：taskId={}, documentId={}, reason={}",
                    taskId,
                    documentId,
                    ex.getMessage(),
                    ex);

            knowledgeDocumentService.markIngestFailed(documentId, ex.getMessage());
            documentIngestTaskService.markFailed(taskId, ex.getMessage());
        }
    }

    private List<ChunkVectorRow> buildVectorRows(List<DocumentChunk> chunks, List<float[]> vectors) {
        List<ChunkVectorRow> rows = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            ChunkVectorRow row = new ChunkVectorRow();
            row.setChunkId(chunk.getId());
            row.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
            row.setDocumentId(chunk.getDocumentId());
            row.setUserId(chunk.getUserId());
            row.setChunkIndex(chunk.getChunkIndex());
            row.setEmbedding(vectors.get(i));

            rows.add(row);
        }

        return rows;
    }

    private void validateVectors(List<float[]> vectors, int expectedCount) {
        if (vectors == null || vectors.size() != expectedCount) {
            throw new BusinessException(ErrorCode.EMBEDDING_SERVICE_ERROR, "向量数量与分块数量不一致");
        }

        for (float[] vector : vectors) {
            if (vector == null || vector.length != embeddingClient.dimension()) {
                throw new BusinessException(ErrorCode.EMBEDDING_DIMENSION_MISMATCH);
            }
        }
    }

    /**
     * 内部入库任务提交入口。
     *
     * <p>用于 LabLink 文件自动解析后的异步入库，不依赖 UserContext。</p>
     *
     * @return taskId；如果文档已经入库成功，则返回 null。
     */
    public Long submitIngestTaskInternal(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);

        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        if (!ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }

        if (IngestStatusEnum.SUCCESS.getCode().equals(document.getIngestStatus())) {
            log.info("LabLink 文档已经入库成功，跳过重复入库：documentId={}", documentId);
            return null;
        }

        DocumentIngestTask runningTask = documentIngestTaskService.getRunningTask(
                document.getId(),
                document.getUserId()
        );

        if (runningTask != null) {
            log.info("LabLink 文档已有运行中的入库任务，跳过重复提交：documentId={}, taskId={}",
                    documentId,
                    runningTask.getId());
            return runningTask.getId();
        }

        DocumentIngestTask task = documentIngestTaskService.createTask(document);

        documentIngestTaskExecutor.execute(() -> executeTask(task.getId()));

        return task.getId();
    }

}