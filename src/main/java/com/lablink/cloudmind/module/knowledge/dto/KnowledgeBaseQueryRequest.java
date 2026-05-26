package com.lablink.cloudmind.module.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeBaseQueryRequest {

    private Long pageNo = 1L;

    private Long pageSize = 10L;

    private String keyword;

    /**
     * 管理页可选筛选：用户ID。
     */
    private Long userId;

    /**
     * 管理页可选筛选：manual / lablink_auto。
     */
    private String sourceType;

    /**
     * 管理页可选筛选：外部用户名，例如 admin / cxy。
     */
    private String externalUsername;

    /**
     * 管理页可选筛选：1 启用，0 禁用。
     * 不传则查询所有未删除知识库。
     */
    private Integer status;
}