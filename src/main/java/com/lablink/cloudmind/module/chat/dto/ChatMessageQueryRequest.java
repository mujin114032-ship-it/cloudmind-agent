package com.lablink.cloudmind.module.chat.dto;

import lombok.Data;

@Data
public class ChatMessageQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 50L;
}