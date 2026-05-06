package com.lablink.cloudmind.module.rag.dto;

import lombok.Data;

@Data
public class RagTraceChunkVO {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    private Boolean hit;

    private String sourceChunkId;

    private Integer distance;

    private Double rerankScore;

    private Double keywordScore; // 关键词分数

    private String recallSource; // vector / keyword / hybrid

    private Integer rankNo;

    private Double score;

    private String textPreview;
}