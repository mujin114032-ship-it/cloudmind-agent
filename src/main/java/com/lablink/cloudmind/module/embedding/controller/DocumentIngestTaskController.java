package com.lablink.cloudmind.module.embedding.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.embedding.application.DocumentAsyncIngestApplicationService;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestTaskVO;
import com.lablink.cloudmind.module.embedding.service.DocumentIngestTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文档异步入库任务接口。
 */
@RestController
@RequiredArgsConstructor
public class DocumentIngestTaskController {

    private final DocumentAsyncIngestApplicationService asyncIngestApplicationService;

    private final DocumentIngestTaskService documentIngestTaskService;

    @PostMapping("/api/knowledge-documents/{documentId}/ingest-async")
    public Result<DocumentIngestTaskVO> submitIngestTask(@PathVariable Long documentId) {
        return Result.success(asyncIngestApplicationService.submitIngestTask(documentId));
    }

    @GetMapping("/api/ingest-tasks/{taskId}")
    public Result<DocumentIngestTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(documentIngestTaskService.getTaskDetail(taskId));
    }

    @GetMapping("/api/knowledge-documents/{documentId}/ingest-task")
    public Result<DocumentIngestTaskVO> getLatestTaskByDocument(@PathVariable Long documentId) {
        return Result.success(documentIngestTaskService.getLatestTaskByDocumentId(documentId));
    }
}