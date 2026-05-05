package com.lablink.cloudmind.module.embedding.enums;

import lombok.Getter;

@Getter
public enum DocumentIngestTaskStatusEnum {

    PENDING(0, "等待中"),
    RUNNING(1, "执行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer code;

    private final String desc;

    DocumentIngestTaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}