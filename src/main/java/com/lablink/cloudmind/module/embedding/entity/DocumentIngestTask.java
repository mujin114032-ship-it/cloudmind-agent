package com.lablink.cloudmind.module.embedding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_ingest_task")
public class DocumentIngestTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private Long knowledgeBaseId;

    private Long userId;

    private Integer status;

    private Integer stage;

    private Integer progress;

    private Integer totalChunks;

    private Integer processedChunks;

    private String modelName;

    private Integer embeddingDim;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}