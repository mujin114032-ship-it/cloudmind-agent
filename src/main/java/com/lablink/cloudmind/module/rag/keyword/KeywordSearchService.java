package com.lablink.cloudmind.module.rag.keyword;

import java.util.List;


public interface KeywordSearchService {

    /**
     * 基于 MySQL LIKE 的轻量关键词召回。
     */
    List<KeywordSearchResult> search(
            Long knowledgeBaseId,
            Long userId,
            String query,
            Integer topK
    );
}