package com.lablink.cloudmind.module.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.chat.dto.ChatSessionQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatSessionVO;
import com.lablink.cloudmind.module.chat.dto.CreateChatSessionRequest;
import com.lablink.cloudmind.module.chat.entity.ChatSession;

public interface ChatSessionService extends IService<ChatSession> {

    ChatSessionVO createSession(CreateChatSessionRequest request);

    PageResult<ChatSessionVO> pageSessions(ChatSessionQueryRequest request);

    ChatSession getCurrentUserSession(Long sessionId);

    void touchSession(Long sessionId, String lastMessage);

    void deleteSession(Long sessionId);
}