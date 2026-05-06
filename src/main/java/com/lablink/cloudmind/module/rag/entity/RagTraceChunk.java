package com.lablink.cloudmind.module.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_trace_chunk")
public class RagTraceChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long traceId;

    private Long chunkId;

    private Long documentId;

    private String fileName;

    private Integer chunkIndex;

    private Integer hit;

    private Long sourceChunkId;

    private Integer distance;

    private Double rerankScore;

    private Double keywordScore; // 关键词分数

    private String recallSource; // vector / keyword / hybrid

    private Integer rankNo;

    private Double score;

    private String textPreview;

    private LocalDateTime createTime;
}