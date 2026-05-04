package com.lablink.cloudmind.module.rag.controller;

import com.lablink.cloudmind.module.rag.application.RagStreamQaApplicationService;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 流式 RAG 问答接口。
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class RagStreamQaController {

    private final RagStreamQaApplicationService ragStreamQaApplicationService;

    @PostMapping(
            value = "/{knowledgeBaseId}/rag/qa/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamQa(
            @PathVariable Long knowledgeBaseId,
            @Valid @RequestBody RagQaRequest request
    ) {
        return ragStreamQaApplicationService.streamQa(knowledgeBaseId, request);
    }
}