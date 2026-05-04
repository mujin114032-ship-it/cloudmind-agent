package com.lablink.cloudmind.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum KnowledgeBaseVisibilityEnum {

    PRIVATE(0, "私有"),
    PUBLIC(1, "公开"),
    TEAM(2, "团队可见");

    private final Integer code;

    private final String desc;

    KnowledgeBaseVisibilityEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}