package com.lablink.cloudmind.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.chat.dto.ChatSessionQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatSessionVO;
import com.lablink.cloudmind.module.chat.dto.CreateChatSessionRequest;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.chat.mapper.ChatSessionMapper;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public ChatSessionVO createSession(CreateChatSessionRequest request) {
        KnowledgeBase kb = knowledgeBaseService.getCurrentUserKnowledgeBase(request.getKnowledgeBaseId());

        ChatSession session = new ChatSession();
        session.setUserId(UserContext.getCurrentUserId());
        session.setKnowledgeBaseId(kb.getId());
        session.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "新会话");
        session.setStatus(1);
        session.setMessageCount(0);
        session.setDeleted(0);

        this.save(session);
        return convertToVO(session);
    }

    @Override
    public ChatSession getCurrentUserSession(Long sessionId) {
        ChatSession session = this.getById(sessionId);
        if (session == null || !UserContext.getCurrentUserId().equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    @Override
    public void touchSession(Long sessionId, String lastMessage) {
        String preview = buildPreview(lastMessage);

        this.lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .set(ChatSession::getLastMessage, preview)
                .set(ChatSession::getLastMessageTime, LocalDateTime.now())
                .setSql("message_count = message_count + 1")
                .update();
    }

    @Override
    public void deleteSession(Long sessionId) {
        ChatSession session = getCurrentUserSession(sessionId);

        this.lambdaUpdate()
                .eq(ChatSession::getId, session.getId())
                .eq(ChatSession::getUserId, UserContext.getCurrentUserId())
                .set(ChatSession::getDeleted, 1)
                .update();
    }

    @Override
    public PageResult<ChatSessionVO> pageSessions(ChatSessionQueryRequest request) {
        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10L : request.getPageSize();

        Page<ChatSession> page = this.lambdaQuery()
                .eq(ChatSession::getUserId, UserContext.getCurrentUserId())
                .eq(request.getKnowledgeBaseId() != null, ChatSession::getKnowledgeBaseId, request.getKnowledgeBaseId())
                .like(StringUtils.hasText(request.getKeyword()), ChatSession::getTitle, request.getKeyword())
                .orderByDesc(ChatSession::getLastMessageTime)
                .orderByDesc(ChatSession::getCreateTime)
                .page(new Page<>(pageNo, pageSize));

        List<ChatSessionVO> records = page.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    private String buildPreview(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= 100 ? text : text.substring(0, 100);
    }

    private ChatSessionVO convertToVO(ChatSession session) {
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
