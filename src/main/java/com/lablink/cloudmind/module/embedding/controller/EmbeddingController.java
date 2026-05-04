package com.lablink.cloudmind.module.embedding.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.embedding.application.DocumentEmbeddingApplicationService;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestVO;
import com.lablink.cloudmind.module.embedding.dto.EmbeddingTestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文档向量化相关接口。
 */
@RestController
@RequestMapping("/api/knowledge-documents")
@RequiredArgsConstructor
public class EmbeddingController {

    private final DocumentEmbeddingApplicationService documentEmbeddingApplicationService;


    /**
     * 对已解析文档的 chunk 执行向量化测试。
     */
    @PostMapping("/{documentId}/embed-test")
    public Result<EmbeddingTestVO> embedTest(@PathVariable Long documentId) {
        return Result.success(documentEmbeddingApplicationService.embedDocumentChunks(documentId));
    }

    @PostMapping("/{documentId}/ingest")
    public Result<DocumentIngestVO> ingest(@PathVariable Long documentId) {
        return Result.success(documentEmbeddingApplicationService.ingestDocumentChunks(documentId));
    }
}
