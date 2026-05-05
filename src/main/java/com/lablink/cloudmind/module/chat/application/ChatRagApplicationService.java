package com.lablink.cloudmind.module.chat.application;

import com.lablink.cloudmind.module.chat.dto.ChatRagQaVO;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.chat.service.ChatMessageService;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import com.lablink.cloudmind.module.rag.application.RagQaApplicationService;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话 RAG 问答应用服务。
 */
@Service
@RequiredArgsConstructor
public class ChatRagApplicationService {

    private final ChatSessionService chatSessionService;

    private final ChatMessageService chatMessageService;

    private final RagQaApplicationService ragQaApplicationService;

    private final RagProperties ragProperties;

    public ChatRagQaVO qa(Long sessionId, RagQaRequest request) {
        ChatSession session = chatSessionService.getCurrentUserSession(sessionId);

        ChatMessage userMessage = chatMessageService.saveUserMessage(session, request.getQuestion());
        chatSessionService.touchSession(session.getId(), request.getQuestion());

        List<ChatMessage> recentMessages = chatMessageService.listRecentMessagesBefore(
                session.getId(),
                userMessage.getId(),
                ragProperties.getQueryRewriteHistoryLimit()
        );

        RagQaVO ragQaVO = ragQaApplicationService.qaWithHistory(
                session.getKnowledgeBaseId(),
                request,
                recentMessages
        );

        ChatMessage assistantMessage = chatMessageService.saveAssistantMessage(session, ragQaVO);
        chatSessionService.touchSession(session.getId(), ragQaVO.getAnswer());

        ChatRagQaVO vo = new ChatRagQaVO();
        vo.setUserMessage(chatMessageService.convertToVO(userMessage));
        vo.setAssistantMessage(chatMessageService.convertToVO(assistantMessage));
        return vo;
    }
}