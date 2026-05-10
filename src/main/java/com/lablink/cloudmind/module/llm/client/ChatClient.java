package com.lablink.cloudmind.module.llm.client;

import com.lablink.cloudmind.module.llm.model.LlmCallContext;

import java.util.function.Consumer;

public interface ChatClient {

    /**
     * 普通非流式问答，使用系统默认配置。
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 普通非流式问答，使用调用上下文中的用户 Key。
     */
    String chat(String systemPrompt, String userPrompt, LlmCallContext context);

    /**
     * 流式问答，使用系统默认配置。
     */
    void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta);

    /**
     * 流式问答，使用调用上下文中的用户 Key。
     */
    void streamChat(String systemPrompt, String userPrompt, LlmCallContext context, Consumer<String> onDelta);

    String modelName();

    default String modelName(LlmCallContext context) {
        if (context != null && context.getModelName() != null && !context.getModelName().isBlank()) {
            return context.getModelName();
        }
        return modelName();
    }
}