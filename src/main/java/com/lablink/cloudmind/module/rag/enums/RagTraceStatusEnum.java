package com.lablink.cloudmind.module.rag.enums;

import lombok.Getter;

@Getter
public enum RagTraceStatusEnum {

    PROCESSING(0, "处理中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    private final Integer code;

    private final String desc;

    RagTraceStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
