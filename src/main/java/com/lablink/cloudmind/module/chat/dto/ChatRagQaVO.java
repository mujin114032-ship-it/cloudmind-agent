package com.lablink.cloudmind.module.chat.dto;

import lombok.Data;

@Data
public class ChatRagQaVO {

    private ChatMessageVO userMessage;

    private ChatMessageVO assistantMessage;
}