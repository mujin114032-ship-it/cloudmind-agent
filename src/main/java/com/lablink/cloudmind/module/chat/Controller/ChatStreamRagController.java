package com.lablink.cloudmind.module.chat.controller;

import com.lablink.cloudmind.module.chat.application.ChatStreamRagApplicationService;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会话 SSE 流式 RAG 问答接口。
 */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatStreamRagController {

    private final ChatStreamRagApplicationService chatStreamRagApplicationService;

    @PostMapping(
            value = "/{sessionId}/rag/qa/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamQa(
            @PathVariable Long sessionId,
            @Valid @RequestBody RagQaRequest request
    ) {
        return chatStreamRagApplicationService.streamQa(sessionId, request);
    }
}