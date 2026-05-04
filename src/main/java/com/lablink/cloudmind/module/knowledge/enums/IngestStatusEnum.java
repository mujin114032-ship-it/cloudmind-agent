package com.lablink.cloudmind.module.knowledge.enums;

import lombok.Getter;

@Getter
public enum IngestStatusEnum {

    NOT_INDEXED(0, "未入库"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "入库成功"),
    FAILED(3, "入库失败");

    private final Integer code;

    private final String desc;

    IngestStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}