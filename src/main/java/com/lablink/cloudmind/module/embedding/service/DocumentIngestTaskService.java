package com.lablink.cloudmind.module.embedding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestTaskVO;
import com.lablink.cloudmind.module.embedding.entity.DocumentIngestTask;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;

public interface DocumentIngestTaskService extends IService<DocumentIngestTask> {

    DocumentIngestTask createTask(KnowledgeDocument document);

    DocumentIngestTask getRunningTask(Long documentId, Long userId);

    DocumentIngestTask getLatestTask(Long documentId, Long userId);

    void markRunning(Long taskId, Integer stage, Integer progress);

    void updateProgress(Long taskId, Integer stage, Integer progress, Integer totalChunks, Integer processedChunks);

    void markSuccess(Long taskId, Integer totalChunks, String modelName, Integer embeddingDim);

    void markFailed(Long taskId, String errorMessage);

    DocumentIngestTaskVO getTaskDetail(Long taskId);

    DocumentIngestTaskVO getLatestTaskByDocumentId(Long documentId);
}