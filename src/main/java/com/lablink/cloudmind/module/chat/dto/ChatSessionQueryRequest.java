package com.lablink.cloudmind.module.chat.dto;

import lombok.Data;

@Data
public class ChatSessionQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 10L;

    private Long knowledgeBaseId;

    private String keyword;
}