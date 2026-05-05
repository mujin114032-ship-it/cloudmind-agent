package com.lablink.cloudmind.module.chat.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.chat.application.ChatRagApplicationService;
import com.lablink.cloudmind.module.chat.dto.ChatRagQaVO;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatRagController {

    private final ChatRagApplicationService chatRagApplicationService;

    @PostMapping("/{sessionId}/rag/qa")
    public Result<ChatRagQaVO> qa(
            @PathVariable Long sessionId,
            @Valid @RequestBody RagQaRequest request
    ) {
        return Result.success(chatRagApplicationService.qa(sessionId, request));
    }
}