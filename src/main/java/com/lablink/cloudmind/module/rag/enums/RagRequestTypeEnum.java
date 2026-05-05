package com.lablink.cloudmind.module.rag.enums;

import lombok.Getter;

@Getter
public enum RagRequestTypeEnum {

    NORMAL(0, "普通问答"),
    STREAM(1, "SSE流式问答");

    private final Integer code;

    private final String desc;

    RagRequestTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}