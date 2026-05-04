package com.lablink.cloudmind.module.llm.dto;

import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible chat completion 响应体。
 */
@Data
public class ChatCompletionResponse {

    private List<Choice> choices;

    @Data
    public static class Choice {
        private ChatMessageDTO message;
    }
}