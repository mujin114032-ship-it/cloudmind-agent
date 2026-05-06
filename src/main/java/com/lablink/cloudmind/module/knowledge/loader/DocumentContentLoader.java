package com.lablink.cloudmind.module.knowledge.loader;

import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;

import java.io.InputStream;

/**
 * 文档内容加载器。
 *
 * <p>不同来源的文档通过不同 Loader 读取，解析服务不直接关心文件来自哪里。</p>
 */
public interface DocumentContentLoader {

    /**
     * 当前 Loader 是否支持该文档。
     */
    boolean supports(KnowledgeDocument document);

    /**
     * 加载文档内容。
     */
    InputStream load(KnowledgeDocument document);

    /**
     * Loader 名称，用于日志和排查问题。
     */
    String loaderName();
}