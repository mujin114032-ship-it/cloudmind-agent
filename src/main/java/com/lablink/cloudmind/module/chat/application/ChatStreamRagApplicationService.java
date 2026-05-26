package com.lablink.cloudmind.module.chat.application;

import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.chat.dto.ChatMessageVO;
import com.lablink.cloudmind.module.chat.entity.ChatMessage;
import com.lablink.cloudmind.module.chat.entity.ChatSession;
import com.lablink.cloudmind.module.chat.service.ChatMessageService;
import com.lablink.cloudmind.module.chat.service.ChatSessionService;
import com.lablink.cloudmind.module.llm.client.ChatClient;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.enums.RagRequestTypeEnum;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.rag.processor.AnswerPostProcessor;
import com.lablink.cloudmind.module.rag.processor.QueryPreprocessor;
import com.lablink.cloudmind.module.rag.prompt.RagPromptBuilder;
import com.lablink.cloudmind.module.rag.rewrite.QueryRewriteService;
import com.lablink.cloudmind.module.rag.service.RagTraceService;
import com.lablink.cloudmind.module.rag.application.RetrievalApplicationService;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.llm.model.LlmCallContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 会话场景下的 SSE 流式 RAG 问答服务。
 *
 * <p>负责保存 user 消息、assistant 生成中消息，并在流式生成完成后更新 AI 消息和引用片段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamRagApplicationService {

    private static final Long SSE_TIMEOUT = 0L;

    private final ChatSessionService chatSessionService;

    private final ChatMessageService chatMessageService;

    private final QueryPreprocessor queryPreprocessor;

    private final RetrievalApplicationService retrievalApplicationService;

    private final RagPromptBuilder ragPromptBuilder;

    private final RagTraceService ragTraceService;

    private final QueryRewriteService queryRewriteService;

    private final RagProperties ragProperties;

    private final AnswerPostProcessor answerPostProcessor;

    private final ChatClient chatClient;

    @Qualifier("ragTaskExecutor")
    private final Executor ragTaskExecutor;

    public SseEmitter streamQa(Long sessionId, RagQaRequest request) {
        long totalStart = System.currentTimeMillis();

        // 这些操作放在请求线程中执行，方便直接校验权限和创建基础记录。
        ChatSession session = chatSessionService.getCurrentUserSession(sessionId);
        String question = queryPreprocessor.preprocess(request.getQuestion());

        int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
        double scoreThreshold = request.getScoreThreshold() == null ? 0.3 : request.getScoreThreshold();

        ChatMessage userMessage = chatMessageService.saveUserMessage(session, question);
        chatSessionService.touchSession(session.getId(), question);

        List<ChatMessage> recentMessages = chatMessageService.listRecentMessagesBefore(
                session.getId(),
                userMessage.getId(),
                ragProperties.getQueryRewriteHistoryLimit()
        );

        String rewrittenQuestion = queryRewriteService.rewriteWithHistory(question, recentMessages);

        String requestPromptVersion = request.getPromptVersion();

        ChatMessage assistantMessage = chatMessageService.saveGeneratingAssistantMessage(session);

        Long traceId = ragTraceService.startTrace(
                session.getKnowledgeBaseId(),
                RagRequestTypeEnum.STREAM.getCode(),
                request.getQuestion(),
                rewrittenQuestion,
                topK,
                scoreThreshold
        );

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        ChatMessageVO userMessageVO = chatMessageService.convertToVO(userMessage);
        ChatMessageVO assistantMessageVO = chatMessageService.convertToVO(assistantMessage);

        sendEvent(emitter, "message_created", Map.of(
                "traceId", String.valueOf(traceId),
                "userMessage", userMessageVO,
                "assistantMessage", assistantMessageVO,
                "rewrittenQuestion", rewrittenQuestion
        ));

        ragTaskExecutor.execute(() -> doStreamQa(
                session,
                assistantMessage,
                traceId,
                question,
                rewrittenQuestion,
                requestPromptVersion,
                topK,
                scoreThreshold,
                request.getSearchMode(),
                null,
                totalStart,
                emitter
        ));

        return emitter;
    }

    public SseEmitter streamQaForLabLink(
            Long userId,
            Long knowledgeBaseId,
            Long sessionId,
            RagQaRequest request,
            LlmCallContext llmCallContext
    ) {
        UserContext.setCurrentUserId(userId);

        try {
            long totalStart = System.currentTimeMillis();

            ChatSession session = getOrCreateLabLinkSession(
                    userId,
                    knowledgeBaseId,
                    sessionId,
                    request.getQuestion()
            );

            String question = queryPreprocessor.preprocess(request.getQuestion());

            int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
            double scoreThreshold = request.getScoreThreshold() == null ? 0.15 : request.getScoreThreshold();

            ChatMessage userMessage = chatMessageService.saveUserMessage(session, question);
            chatSessionService.touchSession(session.getId(), question);

            List<ChatMessage> recentMessages = chatMessageService.listRecentMessagesBefore(
                    session.getId(),
                    userMessage.getId(),
                    ragProperties.getQueryRewriteHistoryLimit()
            );

            String rewrittenQuestion = queryRewriteService.rewriteWithHistory(question, recentMessages);

            String requestPromptVersion = request.getPromptVersion();

            ChatMessage assistantMessage = chatMessageService.saveGeneratingAssistantMessage(session);

            Long traceId = ragTraceService.startTrace(
                    session.getKnowledgeBaseId(),
                    RagRequestTypeEnum.STREAM.getCode(),
                    request.getQuestion(),
                    rewrittenQuestion,
                    topK,
                    scoreThreshold
            );

            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

            ChatMessageVO userMessageVO = chatMessageService.convertToVO(userMessage);
            ChatMessageVO assistantMessageVO = chatMessageService.convertToVO(assistantMessage);

            sendEvent(emitter, "message_created", Map.of(
                    "traceId", String.valueOf(traceId),
                    "sessionId", String.valueOf(session.getId()),
                    "userMessage", userMessageVO,
                    "assistantMessage", assistantMessageVO,
                    "rewrittenQuestion", rewrittenQuestion
            ));

            Long labLinkUserId = userId;

            ragTaskExecutor.execute(() -> {
                UserContext.setCurrentUserId(labLinkUserId);
                try {
                    doStreamQa(
                            session,
                            assistantMessage,
                            traceId,
                            question,
                            rewrittenQuestion,
                            requestPromptVersion,
                            topK,
                            scoreThreshold,
                            request.getSearchMode(),
                            llmCallContext,
                            totalStart,
                            emitter
                    );
                } finally {
                    UserContext.clear();
                }
            });

            return emitter;
        } finally {
            UserContext.clear();
        }
    }

    private ChatSession getOrCreateLabLinkSession(
            Long userId,
            Long knowledgeBaseId,
            Long sessionId,
            String question
    ) {
        if (userId == null || knowledgeBaseId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID或知识库ID不能为空");
        }

        if (sessionId != null) {
            ChatSession session = chatSessionService.getById(sessionId);
            if (session == null
                    || !userId.equals(session.getUserId())
                    || !knowledgeBaseId.equals(session.getKnowledgeBaseId())
                    || Integer.valueOf(1).equals(session.getDeleted())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
            }
            return session;
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setKnowledgeBaseId(knowledgeBaseId);
        session.setTitle(buildSessionTitle(question));
        session.setStatus(1);
        session.setMessageCount(0);
        session.setDeleted(0);

        chatSessionService.save(session);
        return session;
    }

    private String buildSessionTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return "新会话";
        }
        String trimmed = question.trim();
        return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20);
    }

    private void doStreamQa(
            ChatSession session,
            ChatMessage assistantMessage,
            Long traceId,
            String question,
            String rewrittenQuestion,
            String requestPromptVersion,
            int topK,
            double scoreThreshold,
            String searchMode,
            LlmCallContext llmCallContext,
            long totalStart,
            SseEmitter emitter
    ) {
        String traceIdStr = String.valueOf(traceId);
        String assistantMessageIdStr = String.valueOf(assistantMessage.getId());
        StringBuilder answerBuilder = new StringBuilder();

        long retrievalCostMs = 0L;
        long llmCostMs = 0L;

        try {
            sendEvent(emitter, "retrieval_start", Map.of(
                    "traceId", traceIdStr,
                    "assistantMessageId", assistantMessageIdStr,
                    "question", question,
                    "rewrittenQuestion", rewrittenQuestion
            ));

            RetrievalTestRequest retrievalRequest = new RetrievalTestRequest();
            retrievalRequest.setQuery(rewrittenQuestion);
            retrievalRequest.setTopK(topK);
            retrievalRequest.setScoreThreshold(scoreThreshold);
            retrievalRequest.setSearchMode(searchMode);

            long retrievalStart = System.currentTimeMillis();
            RetrievalTestVO retrievalResult = retrievalApplicationService.retrievalTest(
                    session.getKnowledgeBaseId(),
                    retrievalRequest
            );
            retrievalCostMs = System.currentTimeMillis() - retrievalStart;

            List<RetrievedChunkVO> references = retrievalResult.getResults();
            ragTraceService.recordRetrieval(traceId, references, retrievalCostMs);

            Map<String, Object> retrievalDone = new LinkedHashMap<>();
            retrievalDone.put("traceId", traceIdStr);
            retrievalDone.put("assistantMessageId", assistantMessageIdStr);
            retrievalDone.put("question", question);
            retrievalDone.put("rewrittenQuestion", rewrittenQuestion);
            retrievalDone.put("resultCount", references.size());
            retrievalDone.put("retrievalCostMs", retrievalCostMs);
            retrievalDone.put("references", references);
            sendEvent(emitter, "retrieval_done", retrievalDone);

            String promptVersion = ragPromptBuilder.resolvePromptVersion(requestPromptVersion);

            String systemPrompt = ragPromptBuilder.buildSystemPrompt(promptVersion);
            String userPrompt = ragPromptBuilder.buildUserPrompt(
                    promptVersion,
                    question,
                    rewrittenQuestion,
                    references
            );

            ragTraceService.recordPrompt(traceId, promptVersion, systemPrompt, userPrompt);

            sendEvent(emitter, "answer_start", Map.of(
                    "traceId", traceIdStr,
                    "assistantMessageId", assistantMessageIdStr,
                    "modelName", chatClient.modelName(llmCallContext),
                    "promptVersion", promptVersion
            ));

            long llmStart = System.currentTimeMillis();

            chatClient.streamChat(systemPrompt, userPrompt, llmCallContext, delta -> {
                answerBuilder.append(delta);

                Map<String, Object> deltaData = new LinkedHashMap<>();
                deltaData.put("traceId", traceIdStr);
                deltaData.put("assistantMessageId", assistantMessageIdStr);
                deltaData.put("content", delta);

                sendEvent(emitter, "answer_delta", deltaData);
            });

            llmCostMs = System.currentTimeMillis() - llmStart;

            String rawAnswer = answerBuilder.toString().trim();
            String finalAnswer = answerPostProcessor.process(rawAnswer, references);
            long totalCostMs = System.currentTimeMillis() - totalStart;

            ragTraceService.markSuccess(
                    traceId,
                    chatClient.modelName(llmCallContext),
                    finalAnswer,
                    llmCostMs,
                    totalCostMs
            );

            chatMessageService.updateAssistantMessageSuccess(
                    assistantMessage.getId(),
                    finalAnswer,
                    traceId,
                    retrievalCostMs,
                    llmCostMs,
                    totalCostMs,
                    references
            );

            chatSessionService.touchSession(session.getId(), finalAnswer);

            ChatMessage updatedAssistantMessage = chatMessageService.getById(assistantMessage.getId());
            ChatMessageVO assistantMessageVO = chatMessageService.convertToVO(updatedAssistantMessage);

            Map<String, Object> doneData = new LinkedHashMap<>();
            doneData.put("traceId", traceIdStr);
            doneData.put("assistantMessageId", assistantMessageIdStr);
            doneData.put("answer", finalAnswer);
            doneData.put("assistantMessage", assistantMessageVO);
            doneData.put("modelName", chatClient.modelName(llmCallContext));
            doneData.put("promptVersion", promptVersion);
            doneData.put("retrievalCostMs", retrievalCostMs);
            doneData.put("llmCostMs", llmCostMs);
            doneData.put("totalCostMs", totalCostMs);
            doneData.put("references", references);

            sendEvent(emitter, "answer_done", doneData);

            emitter.complete();
        } catch (Exception ex) {
            log.warn("会话 SSE RAG 问答失败：sessionId={}, assistantMessageId={}, reason={}",
                    session.getId(),
                    assistantMessage.getId(),
                    ex.getMessage(),
                    ex);

            String partialAnswer = answerBuilder.toString().trim();
            long totalCostMs = System.currentTimeMillis() - totalStart;

            ragTraceService.markFailed(traceId, ex.getMessage(), totalCostMs);
            chatMessageService.updateAssistantMessageFailed(assistantMessage.getId(), ex.getMessage(), partialAnswer);
            chatSessionService.touchSession(session.getId(), "回答生成失败");

            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("traceId", traceIdStr);
            errorData.put("assistantMessageId", assistantMessageIdStr);
            errorData.put("message", ex.getMessage());

            sendEvent(emitter, "error", errorData);
            emitter.complete();
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException ex) {
            throw new RuntimeException("SSE 推送失败：" + ex.getMessage(), ex);
        }
    }
}