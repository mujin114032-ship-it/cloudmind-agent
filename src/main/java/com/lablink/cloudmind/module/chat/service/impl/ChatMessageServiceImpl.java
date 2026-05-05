package com.lablink.cloudmind.module.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lablink.cloudmind.module.chat.dto.ChatMessageReferenceVO;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.chat.dto.ChatMessageQueryRequest;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.entity.ChatMessageReference;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.chat.enums.ChatMessageStatusEnum;
import com.lablink.cloudmind.module.chat.enums.ChatRoleEnum;
import com.lablink.cloudmind.module.chat.mapper.ChatMessageMapper;
import com.lablink.cloudmind.module.chat.mapper.ChatMessageReferenceMapper;
import com.lablink.cloudmind.module.chat.service.ChatMessageService;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    private final ChatMessageReferenceMapper referenceMapper;

    private final ChatSessionService chatSessionService;

    @Override
    public ChatMessage saveUserMessage(ChatSession session, String question) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(session.getUserId());
        message.setKnowledgeBaseId(session.getKnowledgeBaseId());
        message.setRole(ChatRoleEnum.USER.getCode());
        message.setContent(question);
        message.setStatus(ChatMessageStatusEnum.SUCCESS.getCode());
        message.setDeleted(0);

        this.save(message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage saveAssistantMessage(ChatSession session, RagQaVO ragQaVO) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(session.getUserId());
        message.setKnowledgeBaseId(session.getKnowledgeBaseId());
        message.setRole(ChatRoleEnum.ASSISTANT.getCode());
        message.setContent(ragQaVO.getAnswer());
        message.setTraceId(Long.valueOf(ragQaVO.getTraceId()));
        message.setStatus(ChatMessageStatusEnum.SUCCESS.getCode());
        message.setRetrievalCostMs(ragQaVO.getRetrievalCostMs());
        message.setLlmCostMs(ragQaVO.getLlmCostMs());
        message.setTotalCostMs(ragQaVO.getTotalCostMs());
        message.setDeleted(0);

        this.save(message);
        saveReferences(message.getId(), ragQaVO.getReferences());
        return message;
    }

    @Override
    public ChatMessage saveGeneratingAssistantMessage(ChatSession session) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(session.getUserId());
        message.setKnowledgeBaseId(session.getKnowledgeBaseId());
        message.setRole(ChatRoleEnum.ASSISTANT.getCode());
        message.setContent("");
        message.setStatus(ChatMessageStatusEnum.GENERATING.getCode());
        message.setDeleted(0);

        this.save(message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssistantMessageSuccess(
            Long messageId,
            String answer,
            Long traceId,
            Long retrievalCostMs,
            Long llmCostMs,
            Long totalCostMs,
            List<RetrievedChunkVO> references
    ) {
        this.lambdaUpdate()
                .eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getContent, answer)
                .set(ChatMessage::getTraceId, traceId)
                .set(ChatMessage::getStatus, ChatMessageStatusEnum.SUCCESS.getCode())
                .set(ChatMessage::getRetrievalCostMs, retrievalCostMs)
                .set(ChatMessage::getLlmCostMs, llmCostMs)
                .set(ChatMessage::getTotalCostMs, totalCostMs)
                .set(ChatMessage::getErrorMessage, null)
                .update();

        referenceMapper.delete(
                new LambdaQueryWrapper<ChatMessageReference>()
                        .eq(ChatMessageReference::getMessageId, messageId)
        );

        saveReferences(messageId, references);
    }

    @Override
    public void updateAssistantMessageFailed(Long messageId, String errorMessage, String partialAnswer) {
        String content = org.springframework.util.StringUtils.hasText(partialAnswer)
                ? partialAnswer
                : "回答生成失败";

        this.lambdaUpdate()
                .eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getContent, content)
                .set(ChatMessage::getStatus, ChatMessageStatusEnum.FAILED.getCode())
                .set(ChatMessage::getErrorMessage, errorMessage)
                .update();
    }

    @Override
    public PageResult<ChatMessageVO> pageMessages(Long sessionId, ChatMessageQueryRequest request) {
        // 先校验会话归属，防止跨用户读取消息
        chatSessionService.getCurrentUserSession(sessionId);

        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 50L : request.getPageSize();

        Page<ChatMessage> page = this.lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .page(new Page<>(pageNo, pageSize));

        List<ChatMessage> messages = page.getRecords();
        if (messages.isEmpty()) {
            return PageResult.of(List.of(), page.getTotal(), pageNo, pageSize);
        }

        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();

        Map<Long, List<ChatMessageReference>> referenceMap = referenceMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageReference>()
                                .in(ChatMessageReference::getMessageId, messageIds)
                                .orderByAsc(ChatMessageReference::getRankNo)
                )
                .stream()
                .collect(Collectors.groupingBy(ChatMessageReference::getMessageId));

        List<ChatMessageVO> records = messages.stream()
                .map(message -> convertToVO(message, referenceMap.getOrDefault(message.getId(), Collections.emptyList())))
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public ChatMessageVO convertToVO(ChatMessage message) {
        List<ChatMessageReference> references = referenceMapper.selectList(
                new LambdaQueryWrapper<ChatMessageReference>()
                        .eq(ChatMessageReference::getMessageId, message.getId())
                        .orderByAsc(ChatMessageReference::getRankNo)
        );
        return convertToVO(message, references);
    }

    @Override
    public List<ChatMessage> listRecentMessages(Long sessionId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 6 : Math.min(limit, 20);

        List<ChatMessage> messages = this.lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();

        java.util.Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<ChatMessage> listRecentMessagesBefore(Long sessionId, Long beforeMessageId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 6 : Math.min(limit, 20);

        List<ChatMessage> messages = this.lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .lt(ChatMessage::getId, beforeMessageId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();

        java.util.Collections.reverse(messages);
        return messages;
    }

    private ChatMessageVO convertToVO(ChatMessage message, List<ChatMessageReference> references) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(String.valueOf(message.getId()));
        vo.setSessionId(String.valueOf(message.getSessionId()));
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setTraceId(message.getTraceId() == null ? null : String.valueOf(message.getTraceId()));
        vo.setStatus(message.getStatus());
        vo.setErrorMessage(message.getErrorMessage());
        vo.setRetrievalCostMs(message.getRetrievalCostMs());
        vo.setLlmCostMs(message.getLlmCostMs());
        vo.setTotalCostMs(message.getTotalCostMs());
        vo.setCreateTime(message.getCreateTime());

        List<ChatMessageReferenceVO> referenceVOList = references == null ? List.of() :
                references.stream()
                        .map(this::convertReferenceToVO)
                        .toList();

        vo.setReferences(referenceVOList);
        return vo;
    }

    private ChatMessageReferenceVO convertReferenceToVO(ChatMessageReference reference) {
        ChatMessageReferenceVO vo = new ChatMessageReferenceVO();
        vo.setChunkId(String.valueOf(reference.getChunkId()));
        vo.setDocumentId(String.valueOf(reference.getDocumentId()));
        vo.setFileName(reference.getFileName());
        vo.setChunkIndex(reference.getChunkIndex());
        vo.setHit(reference.getHit() != null && reference.getHit() == 1);
        vo.setSourceChunkId(reference.getSourceChunkId() == null ? null : String.valueOf(reference.getSourceChunkId()));
        vo.setDistance(reference.getDistance());
        vo.setRankNo(reference.getRankNo());
        vo.setScore(reference.getScore());
        vo.setTextPreview(reference.getTextPreview());
        return vo;
    }

    private void saveReferences(Long messageId, List<RetrievedChunkVO> references) {
        if (references == null || references.isEmpty()) {
            return;
        }

        for (int i = 0; i < references.size(); i++) {
            RetrievedChunkVO ref = references.get(i);

            ChatMessageReference entity = new ChatMessageReference();
            entity.setMessageId(messageId);
            entity.setChunkId(Long.valueOf(ref.getChunkId()));
            entity.setDocumentId(Long.valueOf(ref.getDocumentId()));
            entity.setFileName(ref.getFileName());
            entity.setChunkIndex(ref.getChunkIndex());
            entity.setHit(Boolean.TRUE.equals(ref.getHit()) ? 1 : 0);
            entity.setSourceChunkId(
                    ref.getSourceChunkId() == null ? null : Long.valueOf(ref.getSourceChunkId())
            );
            entity.setDistance(ref.getDistance());
            entity.setRankNo(i + 1);
            entity.setScore(ref.getScore());
            entity.setTextPreview(ref.getTextPreview());

            referenceMapper.insert(entity);
        }
    }
}
