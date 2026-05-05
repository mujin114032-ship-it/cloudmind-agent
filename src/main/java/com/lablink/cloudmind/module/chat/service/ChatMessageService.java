package com.lablink.cloudmind.module.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.chat.dto.ChatMessageQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;

import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    ChatMessage saveUserMessage(ChatSession session, String question);

    ChatMessage saveAssistantMessage(ChatSession session, RagQaVO ragQaVO);

    ChatMessage saveGeneratingAssistantMessage(ChatSession session);

    void updateAssistantMessageSuccess(
            Long messageId,
            String answer,
            Long traceId,
            Long retrievalCostMs,
            Long llmCostMs,
            Long totalCostMs,
            List<RetrievedChunkVO> references
    );

    void updateAssistantMessageFailed(Long messageId, String errorMessage, String partialAnswer);

    default void updateAssistantMessageFailed(Long messageId, String errorMessage) {
        updateAssistantMessageFailed(messageId, errorMessage, null);
    }

    PageResult<ChatMessageVO> pageMessages(Long sessionId, ChatMessageQueryRequest request);

    ChatMessageVO convertToVO(ChatMessage message);

    List<ChatMessage> listRecentMessages(Long sessionId, Integer limit);

    List<ChatMessage> listRecentMessagesBefore(Long sessionId, Long beforeMessageId, Integer limit);

}