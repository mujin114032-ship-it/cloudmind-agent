package com.lablink.cloudmind.module.rag.keyword;

import lombok.Data;

/**
 * 关键词召回结果。
 */
@Data
public class KeywordSearchResult {

    private Long chunkId;

    /**
     * 简单关键词得分。
     *
     * <p>第一版使用命中词数量和完整短语命中进行粗略评分。</p>
     */
    private Double keywordScore;
}