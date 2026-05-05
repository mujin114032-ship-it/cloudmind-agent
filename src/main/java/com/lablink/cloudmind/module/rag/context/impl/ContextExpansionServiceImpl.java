package com.lablink.cloudmind.module.rag.context.impl;

import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.context.ContextExpansionService;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 相邻 Chunk 扩展服务。
 *
 * <p>用于在向量命中 chunk 的基础上，补充前后相邻 chunk，提升 Prompt 上下文完整性。</p>
 */
@Service
@RequiredArgsConstructor
public class ContextExpansionServiceImpl implements ContextExpansionService {

    private final RagProperties ragProperties;

    private final DocumentChunkService documentChunkService;

    private final KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public List<RetrievedChunkVO> expand(List<RetrievedChunkVO> hitChunks) {
        if (CollectionUtils.isEmpty(hitChunks)) {
            return List.of();
        }

        if (!Boolean.TRUE.equals(ragProperties.getContextExpansionEnabled())) {
            markHitChunks(hitChunks);
            return hitChunks;
        }

        int before = safeValue(ragProperties.getContextWindowBefore(), 1);
        int after = safeValue(ragProperties.getContextWindowAfter(), 1);
        int maxContextChunks = safeValue(ragProperties.getMaxContextChunks(), 15);

        Map<Long, String> fileNameCache = new LinkedHashMap<>();
        LinkedHashMap<Long, RetrievedChunkVO> resultMap = new LinkedHashMap<>();

        for (RetrievedChunkVO hit : hitChunks) {
            Long hitChunkId = Long.valueOf(hit.getChunkId());
            Long documentId = Long.valueOf(hit.getDocumentId());
            Integer chunkIndex = hit.getChunkIndex();

            int startIndex = Math.max(0, chunkIndex - before);
            int endIndex = chunkIndex + after;

            List<DocumentChunk> neighbors = documentChunkService.listEnabledChunksByDocumentAndIndexRange(
                    documentId,
                    startIndex,
                    endIndex
            );

            for (DocumentChunk neighbor : neighbors) {
                boolean isHit = neighbor.getId().equals(hitChunkId);

                RetrievedChunkVO vo = buildExpandedChunkVO(
                        neighbor,
                        hit,
                        isHit,
                        fileNameCache
                );

                RetrievedChunkVO existing = resultMap.get(neighbor.getId());

                if (existing == null) {
                    resultMap.put(neighbor.getId(), vo);
                    continue;
                }

                // 如果一个 chunk 先作为扩展块出现，后面又作为真正命中块出现，则升级为命中块。
                if (!Boolean.TRUE.equals(existing.getHit()) && isHit) {
                    resultMap.put(neighbor.getId(), vo);
                }
            }
        }

        return resultMap.values()
                .stream()
                .toList();
    }

    private void markHitChunks(List<RetrievedChunkVO> hitChunks) {
        for (RetrievedChunkVO hit : hitChunks) {
            hit.setHit(true);
            hit.setSourceChunkId(hit.getChunkId());
            hit.setDistance(0);
        }
    }

    private RetrievedChunkVO buildExpandedChunkVO(
            DocumentChunk chunk,
            RetrievedChunkVO sourceHit,
            boolean isHit,
            Map<Long, String> fileNameCache
    ) {
        String fileName = fileNameCache.computeIfAbsent(
                chunk.getDocumentId(),
                this::getFileName
        );

        RetrievedChunkVO vo = new RetrievedChunkVO();
        vo.setChunkId(String.valueOf(chunk.getId()));
        vo.setDocumentId(String.valueOf(chunk.getDocumentId()));
        vo.setFileName(fileName);
        vo.setChunkIndex(chunk.getChunkIndex());

        // 扩展 chunk 沿用触发扩展的命中 chunk 分数，方便前端展示和 Trace 分析。
        vo.setScore(sourceHit.getScore());

        vo.setHit(isHit);
        vo.setSourceChunkId(sourceHit.getChunkId());
        vo.setDistance(chunk.getChunkIndex() - sourceHit.getChunkIndex());
        vo.setChunkHash(chunk.getChunkHash());
        vo.setChunkText(chunk.getChunkText());
        vo.setTextPreview(buildPreview(chunk.getChunkText()));

        return vo;
    }

    private String getFileName(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        return document == null ? null : document.getFileName();
    }

    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        int maxLength = 200;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private int safeValue(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }
}