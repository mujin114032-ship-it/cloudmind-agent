package com.lablink.cloudmind.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum KnowledgeBaseStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;

    private final String desc;

    KnowledgeBaseStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}