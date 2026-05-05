package com.lablink.cloudmind.module.chat.dto;

import lombok.Data;

@Data
public class CreateChatSessionRequest {

    private Long knowledgeBaseId;

    private String title;
}