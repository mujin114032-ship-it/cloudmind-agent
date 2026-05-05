package com.lablink.cloudmind.module.chat.dto;

import lombok.Data;

@Data
public class ChatMessageReferenceVO {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    private Boolean hit;

    private String sourceChunkId;

    private Integer distance;

    private Integer rankNo;

    private Double score;

    private String textPreview;
}