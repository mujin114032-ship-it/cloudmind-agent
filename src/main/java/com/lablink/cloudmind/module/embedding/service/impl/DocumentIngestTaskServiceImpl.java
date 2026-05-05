package com.lablink.cloudmind.module.embedding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestTaskVO;
import com.lablink.cloudmind.module.embedding.entity.DocumentIngestTask;
import com.lablink.cloudmind.module.embedding.enums.DocumentIngestTaskStageEnum;
import com.lablink.cloudmind.module.embedding.enums.DocumentIngestTaskStatusEnum;
import com.lablink.cloudmind.module.embedding.mapper.DocumentIngestTaskMapper;
import com.lablink.cloudmind.module.embedding.service.DocumentIngestTaskService;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentIngestTaskServiceImpl extends ServiceImpl<DocumentIngestTaskMapper, DocumentIngestTask>
        implements DocumentIngestTaskService {

    @Override
    public DocumentIngestTask createTask(KnowledgeDocument document) {
        DocumentIngestTask task = new DocumentIngestTask();
        task.setDocumentId(document.getId());
        task.setKnowledgeBaseId(document.getKnowledgeBaseId());
        task.setUserId(document.getUserId());
        task.setStatus(DocumentIngestTaskStatusEnum.PENDING.getCode());
        task.setStage(DocumentIngestTaskStageEnum.WAITING.getCode());
        task.setProgress(0);
        task.setTotalChunks(document.getChunkCount() == null ? 0 : document.getChunkCount());
        task.setProcessedChunks(0);
        task.setModelName(document.getEmbeddingModel());
        task.setEmbeddingDim(document.getEmbeddingDim());
        task.setDeleted(0);

        this.save(task);
        return task;
    }

    @Override
    public DocumentIngestTask getRunningTask(Long documentId, Long userId) {
        return this.lambdaQuery()
                .eq(DocumentIngestTask::getDocumentId, documentId)
                .eq(DocumentIngestTask::getUserId, userId)
                .in(DocumentIngestTask::getStatus, List.of(
                        DocumentIngestTaskStatusEnum.PENDING.getCode(),
                        DocumentIngestTaskStatusEnum.RUNNING.getCode()
                ))
                .orderByDesc(DocumentIngestTask::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public DocumentIngestTask getLatestTask(Long documentId, Long userId) {
        return this.lambdaQuery()
                .eq(DocumentIngestTask::getDocumentId, documentId)
                .eq(DocumentIngestTask::getUserId, userId)
                .orderByDesc(DocumentIngestTask::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public void markRunning(Long taskId, Integer stage, Integer progress) {
        this.lambdaUpdate()
                .eq(DocumentIngestTask::getId, taskId)
                .set(DocumentIngestTask::getStatus, DocumentIngestTaskStatusEnum.RUNNING.getCode())
                .set(DocumentIngestTask::getStage, stage)
                .set(DocumentIngestTask::getProgress, progress)
                .set(DocumentIngestTask::getStartTime, LocalDateTime.now())
                .set(DocumentIngestTask::getErrorMessage, null)
                .update();
    }

    @Override
    public void updateProgress(Long taskId, Integer stage, Integer progress, Integer totalChunks, Integer processedChunks) {
        this.lambdaUpdate()
                .eq(DocumentIngestTask::getId, taskId)
                .set(DocumentIngestTask::getStage, stage)
                .set(DocumentIngestTask::getProgress, progress)
                .set(DocumentIngestTask::getTotalChunks, totalChunks)
                .set(DocumentIngestTask::getProcessedChunks, processedChunks)
                .update();
    }

    @Override
    public void markSuccess(Long taskId, Integer totalChunks, String modelName, Integer embeddingDim) {
        this.lambdaUpdate()
                .eq(DocumentIngestTask::getId, taskId)
                .set(DocumentIngestTask::getStatus, DocumentIngestTaskStatusEnum.SUCCESS.getCode())
                .set(DocumentIngestTask::getStage, DocumentIngestTaskStageEnum.FINISHED.getCode())
                .set(DocumentIngestTask::getProgress, 100)
                .set(DocumentIngestTask::getProcessedChunks, totalChunks)
                .set(DocumentIngestTask::getTotalChunks, totalChunks)
                .set(DocumentIngestTask::getModelName, modelName)
                .set(DocumentIngestTask::getEmbeddingDim, embeddingDim)
                .set(DocumentIngestTask::getFinishTime, LocalDateTime.now())
                .set(DocumentIngestTask::getErrorMessage, null)
                .update();
    }

    @Override
    public void markFailed(Long taskId, String errorMessage) {
        this.lambdaUpdate()
                .eq(DocumentIngestTask::getId, taskId)
                .set(DocumentIngestTask::getStatus, DocumentIngestTaskStatusEnum.FAILED.getCode())
                .set(DocumentIngestTask::getFinishTime, LocalDateTime.now())
                .set(DocumentIngestTask::getErrorMessage, errorMessage)
                .update();
    }

    @Override
    public DocumentIngestTaskVO getTaskDetail(Long taskId) {
        DocumentIngestTask task = this.getById(taskId);
        if (task == null || !UserContext.getCurrentUserId().equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.INGEST_TASK_NOT_FOUND);
        }

        return convertToVO(task);
    }

    @Override
    public DocumentIngestTaskVO getLatestTaskByDocumentId(Long documentId) {
        Long userId = UserContext.getCurrentUserId();

        DocumentIngestTask task = getLatestTask(documentId, userId);
        if (task == null) {
            throw new BusinessException(ErrorCode.INGEST_TASK_NOT_FOUND);
        }

        return convertToVO(task);
    }

    private DocumentIngestTaskVO convertToVO(DocumentIngestTask task) {
        DocumentIngestTaskVO vo = new DocumentIngestTaskVO();
        vo.setTaskId(String.valueOf(task.getId()));
        vo.setDocumentId(String.valueOf(task.getDocumentId()));
        vo.setKnowledgeBaseId(String.valueOf(task.getKnowledgeBaseId()));
        vo.setStatus(task.getStatus());
        vo.setStage(task.getStage());
        vo.setProgress(task.getProgress());
        vo.setTotalChunks(task.getTotalChunks());
        vo.setProcessedChunks(task.getProcessedChunks());
        vo.setModelName(task.getModelName());
        vo.setEmbeddingDim(task.getEmbeddingDim());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartTime(task.getStartTime());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }
}