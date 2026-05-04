package com.lablink.cloudmind.module.llm.client;

import java.util.function.Consumer;

public interface ChatClient {

    /**
     * 普通非流式问答。
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式问答。
     *
     * <p>onDelta 每次接收大模型返回的一段增量文本。</p>
     */
    void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta);

    String modelName();
}