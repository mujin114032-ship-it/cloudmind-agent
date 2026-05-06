package com.lablink.cloudmind.module.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message_reference")
public class ChatMessageReference {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long messageId;

    private Long chunkId;

    private Long documentId;

    private String fileName;

    private Integer chunkIndex;

    private Integer hit;

    private Long sourceChunkId;

    private Integer distance;

    private Double rerankScore;

    private Integer rankNo;

    private Double score;

    private Double keywordScore; // 关键词分数

    private String recallSource; // vector / keyword / hybrid

    private String textPreview;

    private LocalDateTime createTime;
}