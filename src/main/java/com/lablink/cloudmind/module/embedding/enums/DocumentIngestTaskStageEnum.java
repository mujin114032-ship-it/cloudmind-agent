package com.lablink.cloudmind.module.embedding.enums;

import lombok.Getter;

@Getter
public enum DocumentIngestTaskStageEnum {

    WAITING(0, "等待中"),
    VALIDATING(1, "校验中"),
    EMBEDDING(2, "向量化中"),
    WRITING_VECTOR(3, "写入向量库中"),
    UPDATING_STATUS(4, "更新状态中"),
    FINISHED(5, "完成");

    private final Integer code;

    private final String desc;

    DocumentIngestTaskStageEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}