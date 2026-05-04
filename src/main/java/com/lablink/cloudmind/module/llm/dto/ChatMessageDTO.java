package com.lablink.cloudmind.module.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible chat message。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String role;

    private String content;
}