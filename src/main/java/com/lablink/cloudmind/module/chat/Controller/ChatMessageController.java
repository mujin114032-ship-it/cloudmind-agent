package com.lablink.cloudmind.module.chat.controller;

import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.chat.dto.ChatMessageQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/{sessionId}/messages")
    public Result<PageResult<ChatMessageVO>> pageMessages(
            @PathVariable Long sessionId,
            ChatMessageQueryRequest request
    ) {
        return Result.success(chatMessageService.pageMessages(sessionId, request));
    }
}