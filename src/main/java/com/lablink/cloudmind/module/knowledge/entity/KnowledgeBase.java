package com.lablink.cloudmind.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    private Integer visibility;

    private Integer status;

    private Integer documentCount;

    private Integer chunkCount;

    private String embeddingModel;

    private Integer embeddingDim;

    /**
     * 知识库来源：manual / lablink_auto。
     */
    private String sourceType;

    /**
     * 外部系统用户 ID，例如 LabLink userId。
     */
    private String externalUserId;

    /**
     * 外部系统用户名，例如 LabLink username。
     */
    private String externalUsername;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
