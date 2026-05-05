package com.lablink.cloudmind.module.chat.enums;

import lombok.Getter;

@Getter
public enum ChatMessageStatusEnum {

    GENERATING(0, "生成中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败");

    private final Integer code;

    private final String desc;

    ChatMessageStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}