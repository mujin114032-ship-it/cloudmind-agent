package com.lablink.cloudmind.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long knowledgeBaseId;

    private Long documentId;

    private Long userId;

    private Integer chunkIndex;

    private String chunkText;

    private String chunkHash;

    private Integer tokenCount;

    private Integer charCount;

    private String vectorId;

    private String embeddingModel;

    private Integer embeddingDim;

    private Integer enabled;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}