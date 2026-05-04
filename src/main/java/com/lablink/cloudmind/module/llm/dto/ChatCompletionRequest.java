package com.lablink.cloudmind.module.llm.dto;

import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible chat completion 请求体。
 */
@Data
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessageDTO> messages;

    private Double temperature;

    private Boolean stream;
}