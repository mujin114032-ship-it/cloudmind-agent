package com.lablink.cloudmind.module.knowledge.controller;

import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.knowledge.application.KnowledgeDocumentParseApplicationService;
import com.lablink.cloudmind.module.knowledge.dto.AddKnowledgeDocumentRequest;
import com.lablink.cloudmind.module.knowledge.dto.DocumentChunkQueryRequest;
import com.lablink.cloudmind.module.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.knowledge.vo.DocumentChunkVO;
import com.lablink.cloudmind.module.knowledge.vo.KnowledgeDocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final KnowledgeDocumentParseApplicationService parseApplicationService;

    private final DocumentChunkService documentChunkService;

    @PostMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public Result<KnowledgeDocumentVO> addDocumentToKnowledgeBase(
            @PathVariable Long knowledgeBaseId,
            @Valid @RequestBody AddKnowledgeDocumentRequest request
    ) {
        return Result.success(knowledgeDocumentService.addDocumentToKnowledgeBase(knowledgeBaseId, request));
    }

    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public Result<PageResult<KnowledgeDocumentVO>> pageDocuments(
            @PathVariable Long knowledgeBaseId,
            KnowledgeDocumentQueryRequest request
    ) {
        return Result.success(knowledgeDocumentService.pageDocuments(knowledgeBaseId, request));
    }

    @GetMapping("/api/knowledge-documents/{documentId}")
    public Result<KnowledgeDocumentVO> getDocumentDetail(@PathVariable Long documentId) {
        return Result.success(knowledgeDocumentService.getDocumentDetail(documentId));
    }

    @DeleteMapping("/api/knowledge-documents/{documentId}")
    public Result<Void> removeDocument(@PathVariable Long documentId) {
        knowledgeDocumentService.removeDocument(documentId);
        return Result.success();
    }

    @PostMapping(
            value = "/api/knowledge-bases/{knowledgeBaseId}/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Result<KnowledgeDocumentVO> uploadDocumentToKnowledgeBase(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file
    ) {
        return Result.success(knowledgeDocumentService.uploadDocumentToKnowledgeBase(knowledgeBaseId, file));
    }

    @PostMapping("/api/knowledge-documents/{documentId}/parse")
    public Result<Void> parseDocument(@PathVariable Long documentId) {
        parseApplicationService.parseAndChunk(documentId);
        return Result.success();
    }

    @GetMapping("/api/knowledge-documents/{documentId}/chunks")
    public Result<PageResult<DocumentChunkVO>> pageChunks(
            @PathVariable Long documentId,
            DocumentChunkQueryRequest request
    ) {
        return Result.success(documentChunkService.pageChunks(documentId, request));
    }
}