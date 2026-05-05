package com.lablink.cloudmind.module.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_trace")
public class RagTrace {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private Integer requestType;

    private String originalQuestion;

    private String finalQuestion;

    private Integer topK;

    private Double scoreThreshold;

    private Integer resultCount;

    private String promptVersion;

    private String systemPrompt;

    private String userPrompt;

    private String modelName;

    private String answer;

    private Long retrievalCostMs;

    private Long llmCostMs;

    private Long totalCostMs;

    private Integer status;

    private String errorMessage;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
