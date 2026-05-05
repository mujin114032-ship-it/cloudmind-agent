package com.lablink.cloudmind.module.rag.filter.impl;

import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.filter.RetrievalResultFilterService;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 检索结果过滤服务。
 *
 * <p>负责对向量命中结果和扩展上下文结果进行去重、低分过滤和数量控制。</p>
 */
@Service
@RequiredArgsConstructor
public class RetrievalResultFilterServiceImpl implements RetrievalResultFilterService {

    private final RagProperties ragProperties;

    @Override
    public List<RetrievedChunkVO> filterHitChunks(
            List<RetrievedChunkVO> hitChunks,
            Double requestScoreThreshold
    ) {
        if (CollectionUtils.isEmpty(hitChunks)) {
            return List.of();
        }

        double effectiveThreshold = effectiveScoreThreshold(requestScoreThreshold);
        int maxPerDocument = safePositive(ragProperties.getMaxHitChunksPerDocument(), 3);

        Set<String> dedupKeys = new HashSet<>();
        Map<String, Integer> documentHitCountMap = new HashMap<>();
        List<RetrievedChunkVO> results = new ArrayList<>();

        for (RetrievedChunkVO chunk : hitChunks) {
            if (chunk == null) {
                continue;
            }

            if (chunk.getScore() == null || chunk.getScore() < effectiveThreshold) {
                continue;
            }

            String dedupKey = buildDedupKey(chunk);
            if (Boolean.TRUE.equals(ragProperties.getRemoveDuplicateChunksEnabled())
                    && dedupKeys.contains(dedupKey)) {
                continue;
            }

            String documentId = chunk.getDocumentId();
            int currentDocCount = documentHitCountMap.getOrDefault(documentId, 0);
            if (currentDocCount >= maxPerDocument) {
                continue;
            }

            chunk.setHit(true);
            chunk.setSourceChunkId(chunk.getChunkId());
            chunk.setDistance(0);

            dedupKeys.add(dedupKey);
            documentHitCountMap.put(documentId, currentDocCount + 1);
            results.add(chunk);
        }

        return results;
    }

    @Override
    public List<RetrievedChunkVO> filterContextChunks(List<RetrievedChunkVO> contextChunks) {
        if (CollectionUtils.isEmpty(contextChunks)) {
            return List.of();
        }

        int maxContextChunks = safePositive(ragProperties.getMaxContextChunks(), 15);

        LinkedHashMap<String, RetrievedChunkVO> resultMap = new LinkedHashMap<>();

        for (RetrievedChunkVO chunk : contextChunks) {
            if (chunk == null) {
                continue;
            }

            String dedupKey = buildDedupKey(chunk);
            RetrievedChunkVO existing = resultMap.get(dedupKey);

            if (existing == null) {
                resultMap.put(dedupKey, chunk);
                continue;
            }

            // 如果一个片段先作为扩展块出现，后面又作为直接命中块出现，则升级为直接命中。
            if (!Boolean.TRUE.equals(existing.getHit()) && Boolean.TRUE.equals(chunk.getHit())) {
                resultMap.put(dedupKey, chunk);
            }
        }

        return resultMap.values()
                .stream()
                .limit(maxContextChunks)
                .toList();
    }

    private double effectiveScoreThreshold(Double requestScoreThreshold) {
        double requestThreshold = requestScoreThreshold == null ? 0.0 : requestScoreThreshold;
        double systemThreshold = ragProperties.getRetrievalMinScoreThreshold() == null
                ? 0.0
                : ragProperties.getRetrievalMinScoreThreshold();

        return Math.max(requestThreshold, systemThreshold);
    }

    private String buildDedupKey(RetrievedChunkVO chunk) {
        if (Boolean.TRUE.equals(ragProperties.getRemoveDuplicateChunksEnabled())
                && StringUtils.hasText(chunk.getChunkHash())) {
            return "hash:" + chunk.getChunkHash();
        }

        return "chunk:" + chunk.getChunkId();
    }

    private int safePositive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}