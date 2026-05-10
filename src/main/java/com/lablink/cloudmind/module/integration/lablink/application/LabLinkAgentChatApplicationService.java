package com.lablink.cloudmind.module.integration.lablink.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.chat.application.ChatStreamRagApplicationService;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.dto.ChatSessionVO;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.chat.service.ChatMessageService;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import com.lablink.cloudmind.module.integration.lablink.dto.LabLinkAgentChatStreamRequest;
import com.lablink.cloudmind.module.integration.lablink.security.LabLinkUserPrincipal;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.llm.entity.UserLlmCredential;
import com.lablink.cloudmind.module.llm.model.LlmCallContext;
import com.lablink.cloudmind.module.llm.service.UserLlmCredentialService;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.security.service.ApiKeyCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * LabLink Agent 聊天应用服务。
 */
@Service
@RequiredArgsConstructor
public class LabLinkAgentChatApplicationService {

    private final KnowledgeBaseService knowledgeBaseService;

    private final UserLlmCredentialService userLlmCredentialService;

    private final ApiKeyCryptoService apiKeyCryptoService;

    private final ChatSessionService chatSessionService;

    private final ChatMessageService chatMessageService;

    private final ChatStreamRagApplicationService chatStreamRagApplicationService;

    public SseEmitter streamChat(
            LabLinkUserPrincipal principal,
            LabLinkAgentChatStreamRequest request
    ) {
        Long userId = principal.getUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                userId,
                principal.getUsername()
        );

        UserLlmCredential credential = userLlmCredentialService.getEnabledDashScopeCredential(userId);
        if (credential == null) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "请先配置阿里百炼 API Key");
        }

        String apiKey = apiKeyCryptoService.decrypt(credential.getApiKeyCipher());

        LlmCallContext llmCallContext = LlmCallContext.builder()
                .userId(userId)
                .provider(credential.getProvider())
                .apiKey(apiKey)
                .modelName(credential.getModelName())
                .build();

        RagQaRequest ragRequest = new RagQaRequest();
        ragRequest.setQuestion(request.getQuestion());
        ragRequest.setTopK(request.getTopK());
        ragRequest.setScoreThreshold(request.getScoreThreshold());
        ragRequest.setSearchMode(request.getSearchMode());
        ragRequest.setPromptVersion(request.getPromptVersion());

        return chatStreamRagApplicationService.streamQaForLabLink(
                userId,
                knowledgeBase.getId(),
                request.getSessionId(),
                ragRequest,
                llmCallContext
        );
    }

    public List<ChatSessionVO> listSessions(LabLinkUserPrincipal principal) {
        Long userId = principal.getUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                userId,
                principal.getUsername()
        );

        return chatSessionService.lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getKnowledgeBaseId, knowledgeBase.getId())
                .eq(ChatSession::getDeleted, 0)
                .orderByDesc(ChatSession::getLastMessageTime)
                .orderByDesc(ChatSession::getCreateTime)
                .list()
                .stream()
                .map(this::convertSessionToVO)
                .toList();
    }

    public List<ChatMessageVO> listMessages(LabLinkUserPrincipal principal, Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "会话ID不能为空");
        }

        Long userId = principal.getUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                userId,
                principal.getUsername()
        );

        ChatSession session = chatSessionService.getById(sessionId);

        if (session == null
                || !userId.equals(session.getUserId())
                || !knowledgeBase.getId().equals(session.getKnowledgeBaseId())
                || Integer.valueOf(1).equals(session.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }

        return chatMessageService.lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getDeleted, 0)
                .orderByAsc(ChatMessage::getCreateTime)
                .list()
                .stream()
                .map(chatMessageService::convertToVO)
                .toList();
    }

    public void deleteSession(LabLinkUserPrincipal principal, Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "会话ID不能为空");
        }

        Long userId = principal.getUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.ensureLabLinkPrivateKnowledgeBase(
                userId,
                principal.getUsername()
        );

        ChatSession session = chatSessionService.getById(sessionId);

        if (session == null
                || !userId.equals(session.getUserId())
                || !knowledgeBase.getId().equals(session.getKnowledgeBaseId())
                || Integer.valueOf(1).equals(session.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }

        chatSessionService.lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getKnowledgeBaseId, knowledgeBase.getId())
                .set(ChatSession::getDeleted, 1)
                .update();
    }

    private ChatSessionVO convertSessionToVO(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setSessionId(String.valueOf(session.getId()));
        vo.setKnowledgeBaseId(String.valueOf(session.getKnowledgeBaseId()));
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setMessageCount(session.getMessageCount());
        vo.setLastMessage(session.getLastMessage());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setCreateTime(session.getCreateTime());
        return vo;
    }
}