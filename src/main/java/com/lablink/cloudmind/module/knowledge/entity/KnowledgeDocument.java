package com.lablink.cloudmind.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long knowledgeBaseId;

    private Long userId;

    private Long fileId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String parserType;

    private Integer parseStatus;

    private Integer ingestStatus;

    private Integer chunkCount;

    private String embeddingModel;

    private Integer embeddingDim;

    private String errorMessage;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String storageType;

    private String storagePath;

    private String contentType;

    /**
     * 文档来源：upload / lablink_file / remote_url。
     */
    private String sourceType;

    /**
     * 外部系统文件 ID，例如 LabLink 的 fileId。
     */
    private String externalFileId;

    /**
     * 外部文件 URL，远程导入时使用。
     */
    private String externalFileUrl;
}