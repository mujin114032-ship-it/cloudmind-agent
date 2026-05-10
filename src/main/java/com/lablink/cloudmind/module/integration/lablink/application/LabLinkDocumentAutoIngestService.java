package com.lablink.cloudmind.module.integration.lablink.application;

import com.lablink.cloudmind.module.embedding.application.DocumentAsyncIngestApplicationService;
import com.lablink.cloudmind.module.knowledge.application.KnowledgeDocumentParseApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * LabLink 同步文档自动解析入库服务。
 *
 * <p>用于将 LabLink 同步过来的文档自动完成：解析 → 分块 → 向量入库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabLinkDocumentAutoIngestService {

    private final KnowledgeDocumentParseApplicationService parseApplicationService;

    private final DocumentAsyncIngestApplicationService asyncIngestApplicationService;

    @Async("documentIngestTaskExecutor")
    public void parseAndIngestAsync(Long documentId) {
        try {
            log.info("开始自动解析并入库 LabLink 文档：documentId={}", documentId);

            parseApplicationService.parseAndChunkInternal(documentId);

            Long taskId = asyncIngestApplicationService.submitIngestTaskInternal(documentId);

            log.info("LabLink 文档自动入库任务已提交：documentId={}, taskId={}",
                    documentId,
                    taskId);
        } catch (Exception ex) {
            log.warn("自动解析入库 LabLink 文档失败：documentId={}, reason={}",
                    documentId,
                    ex.getMessage(),
                    ex);
        }
    }
}