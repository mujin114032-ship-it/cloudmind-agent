package com.lablink.cloudmind.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "用户未登录"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),

    KNOWLEDGE_BASE_NOT_FOUND(41001, "知识库不存在"),
    KNOWLEDGE_DOCUMENT_NOT_FOUND(41002, "知识库文档不存在"),
    KNOWLEDGE_DOCUMENT_ALREADY_EXISTS(41003, "文档已加入该知识库"),

    FILE_NOT_FOUND(42001, "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED(42002, "暂不支持该文件类型"),
    FILE_STORAGE_ERROR(42003, "文件存储失败"),

    EMBEDDING_SERVICE_ERROR(43001, "向量化服务调用失败"),
    VECTOR_STORE_ERROR(43002, "向量数据库操作失败"),
    LLM_SERVICE_ERROR(43003, "大模型服务调用失败"),
    EMBEDDING_DIMENSION_MISMATCH(43004, "向量维度不匹配"),
    RERANKER_SERVICE_ERROR(43005, "Reranker 服务异常"),

    DOCUMENT_PARSE_ERROR(44001, "文档解析失败"),
    DOCUMENT_NOT_PARSEABLE(44002, "当前文档没有可解析的文件内容"),
    DOCUMENT_CHUNK_EMPTY(44003, "文档解析结果为空"),
    DOCUMENT_NOT_PARSED(44004, "文档尚未解析成功"),
    DOCUMENT_CHUNK_NOT_FOUND(44005, "文档分块不存在"),

    INGEST_TASK_NOT_FOUND(45001, "入库任务不存在"),
    INGEST_TASK_RUNNING(45002, "文档入库任务正在执行中"),
    DOCUMENT_ALREADY_INGESTED(45003, "文档已经入库成功"),

    SYSTEM_ERROR(50000, "系统内部异常");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}