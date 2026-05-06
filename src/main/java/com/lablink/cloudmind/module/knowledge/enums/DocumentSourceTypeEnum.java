package com.lablink.cloudmind.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum DocumentSourceTypeEnum {

    UPLOAD("upload", "本地上传"),

    LABLINK_FILE("lablink_file", "LabLink云盘文件"),

    REMOTE_URL("remote_url", "远程URL文件");

    private final String code;

    private final String desc;

    DocumentSourceTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}