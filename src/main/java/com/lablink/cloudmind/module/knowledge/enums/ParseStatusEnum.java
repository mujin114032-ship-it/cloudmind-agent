package com.lablink.cloudmind.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum ParseStatusEnum {

    NOT_PARSED(0, "未解析"),
    PARSING(1, "解析中"),
    SUCCESS(2, "解析成功"),
    FAILED(3, "解析失败");

    private final Integer code;

    private final String desc;

    ParseStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
