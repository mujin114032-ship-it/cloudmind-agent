package com.lablink.cloudmind.module.integration.lablink.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.dto.ChatSessionVO;
import com.lablink.cloudmind.module.integration.lablink.application.LabLinkAgentApplicationService;
import com.lablink.cloudmind.module.integration.lablink.application.LabLinkAgentChatApplicationService;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkAgentBootstrapRequest;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkAgentChatStreamRequest;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkSaveLlmKeyRequest;
import com.lablink.cloudmind.module.integration.lablink.security.LabLinkJwtAuthService;
import com.lablink.cloudmind.module.integration.lablink.security.LabLinkUserPrincipal;
import com.lablink.cloudmind.module.integration.lablink.vo.LabLinkAgentBootstrapVO;
import com.lablink.cloudmind.module.rag.dto.RagTraceDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * LabLink Agent 浏览器接口。
 *
 * <p>前端直接携带 LabLink 登录 Token 访问 CloudMind，避免 SSE 再经过 LabLink 后端代理。</p>
 */
@RestController
@RequestMapping("/api/lablink/agent")
@RequiredArgsConstructor
public class LabLinkAgentBrowserController {

    private final LabLinkJwtAuthService labLinkJwtAuthService;

    private final LabLinkAgentApplicationService labLinkAgentApplicationService;

    private final LabLinkAgentChatApplicationService labLinkAgentChatApplicationService;

    @GetMapping("/bootstrap")
    public Result<LabLinkAgentBootstrapVO> bootstrap(
            @RequestHeader("Authorization") String authorization
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        LabLinkAgentBootstrapRequest request = new LabLinkAgentBootstrapRequest();
        request.setLabLinkUserId(String.valueOf(principal.getUserId()));
        request.setUsername(principal.getUsername());

        return Result.success(
                labLinkAgentApplicationService.bootstrap(request)
        );
    }

    @PostMapping("/llm-key")
    public Result<LabLinkAgentBootstrapVO> saveLlmKey(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SaveLlmKeyBody body
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        LabLinkSaveLlmKeyRequest request = new LabLinkSaveLlmKeyRequest();
        request.setLabLinkUserId(String.valueOf(principal.getUserId()));
        request.setUsername(principal.getUsername());
        request.setApiKey(body.getApiKey());
        request.setModelName(body.getModelName());

        return Result.success(
                labLinkAgentApplicationService.saveLlmKey(request)
        );
    }

    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamChat(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody LabLinkAgentChatStreamRequest request
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        return labLinkAgentChatApplicationService.streamChat(principal, request);
    }

    @Data
    public static class SaveLlmKeyBody {

        @NotBlank(message = "API Key不能为空")
        private String apiKey;

        private String modelName;
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions(
            @RequestHeader("Authorization") String authorization
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        return Result.success(
                labLinkAgentChatApplicationService.listSessions(principal)
        );
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> listMessages(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sessionId
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        return Result.success(
                labLinkAgentChatApplicationService.listMessages(principal, sessionId)
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sessionId
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);

        labLinkAgentChatApplicationService.deleteSession(principal, sessionId);
        return Result.success();
    }

    @GetMapping("/traces/{traceId}")
    public Result<RagTraceDetailVO> getTraceDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long traceId
    ) {
        LabLinkUserPrincipal principal = labLinkJwtAuthService.parseAuthorizationHeader(authorization);
        return Result.success(labLinkAgentChatApplicationService.getTraceDetail(principal, traceId));
    }
}