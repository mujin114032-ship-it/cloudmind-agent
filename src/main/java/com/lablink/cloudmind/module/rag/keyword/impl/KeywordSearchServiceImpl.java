package com.lablink.cloudmind.module.rag.keyword.impl;

import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.rag.keyword.KeywordSearchResult;
import com.lablink.cloudmind.module.rag.keyword.KeywordSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MySQL LIKE 版关键词召回。
 *
 * <p>第一版不引入 ES / Lucene，先用轻量关键词召回补充向量检索。</p>
 */
@Service
@RequiredArgsConstructor
public class KeywordSearchServiceImpl implements KeywordSearchService {

    private final DocumentChunkService documentChunkService;

    @Override
    public List<KeywordSearchResult> search(
            Long knowledgeBaseId,
            Long userId,
            String query,
            Integer topK
    ) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        int limit = topK == null || topK <= 0 ? 20 : Math.min(topK, 50);

        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> candidates = documentChunkService.searchByKeywords(
                knowledgeBaseId,
                userId,
                keywords,
                limit * 3
        );

        if (candidates.isEmpty()) {
            return List.of();
        }

        return candidates.stream()
                .map(chunk -> buildKeywordResult(chunk, query, keywords))
                .sorted(Comparator.comparing(KeywordSearchResult::getKeywordScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private KeywordSearchResult buildKeywordResult(
            DocumentChunk chunk,
            String query,
            List<String> keywords
    ) {
        String text = chunk.getChunkText() == null ? "" : chunk.getChunkText();

        double score = 0.0;

        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                score += 1.0;
            }
        }

        // 完整短语命中，额外加分。
        if (StringUtils.hasText(query) && text.contains(query.trim())) {
            score += 2.0;
        }

        KeywordSearchResult result = new KeywordSearchResult();
        result.setChunkId(chunk.getId());
        result.setKeywordScore(score);
        return result;
    }

    private List<String> extractKeywords(String query) {
        String normalized = query
                .replaceAll("[，。！？、；：,.!?;:()（）\\[\\]【】\"'“”]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        List<String> keywords = new ArrayList<>();

        // 英文、命令、变量名场景。
        String[] parts = normalized.split(" ");
        for (String part : parts) {
            String token = part.trim();
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }

        // 中文问题没有空格时，保留完整 query 作为关键词。
        if (keywords.isEmpty() || normalized.length() <= 20) {
            keywords.add(normalized);
        }

        return keywords.stream()
                .distinct()
                .limit(8)
                .toList();
    }
}