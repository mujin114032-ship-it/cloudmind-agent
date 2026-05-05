package com.lablink.cloudmind.module.rag.rewrite;

import com.lablink.cloudmind.module.chat.entity.ChatMessage;

import java.util.List;

public interface QueryRewriteService {

    /**
     * 无会话历史的问题改写。
     */
    String rewrite(String question);

    /**
     * 结合会话历史的问题改写。
     */
    String rewriteWithHistory(String question, List<ChatMessage> historyMessages);
}