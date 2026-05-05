package com.lablink.cloudmind.module.chat.controller;

import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.chat.dto.ChatSessionQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatSessionVO;
import com.lablink.cloudmind.module.chat.dto.CreateChatSessionRequest;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    public Result<ChatSessionVO> createSession(@Valid @RequestBody CreateChatSessionRequest request) {
        return Result.success(chatSessionService.createSession(request));
    }

    @GetMapping
    public Result<PageResult<ChatSessionVO>> pageSessions(ChatSessionQueryRequest request) {
        return Result.success(chatSessionService.pageSessions(request));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        chatSessionService.deleteSession(sessionId);
        return Result.success();
    }
}