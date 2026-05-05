package com.lablink.cloudmind.module.chat.enums;

import lombok.Getter;

@Getter
public enum ChatRoleEnum {

    USER("user", "用户"),
    ASSISTANT("assistant", "AI助手");

    private final String code;

    private final String desc;

    ChatRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}