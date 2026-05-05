package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.embedding.client.EmbeddingClient;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.rag.context.ContextExpansionService;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.filter.RetrievalResultFilterService;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.vector.model.VectorSearchResult;
import com.lablink.cloudmind.module.vector.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索应用服务。
 *
 * <p>负责串联：问题向量化 → Milvus topK → 基础分数过滤 → 命中 chunk 去重 → 限制单文档命中数量 → 相邻 Chunk 扩展 → 扩展结果再次去重 → 限制最终上下文数量 → Prompt。</p>
 */
@Service
@RequiredArgsConstructor
public class RetrievalApplicationService {

    private final KnowledgeBaseService knowledgeBaseService;

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final ContextExpansionService contextExpansionService;

    private final RetrievalResultFilterService retrievalResultFilterService;

    private final EmbeddingClient embeddingClient;

    private final VectorStoreService vectorStoreService;

    public RetrievalTestVO retrievalTest(Long knowledgeBaseId, RetrievalTestRequest request) {
        long start = System.currentTimeMillis();

        // 先校验知识库归属当前用户
        Long userId = UserContext.getCurrentUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.getCurrentUserKnowledgeBase(knowledgeBaseId);

        int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
        double scoreThreshold = request.getScoreThreshold() == null ? 0.3 : request.getScoreThreshold();

        float[] queryVector = embeddingClient.embedQuery(request.getQuery());

        List<VectorSearchResult> vectorResults = vectorStoreService.search(
                knowledgeBase.getId(),
                userId,
                queryVector,
                topK
        );

        List<VectorSearchResult> filteredVectorResults = vectorResults.stream()
                .filter(item -> item.getScore() != null && item.getScore() >= scoreThreshold)
                .toList();

        List<Long> chunkIds = filteredVectorResults.stream()
                .map(VectorSearchResult::getChunkId)
                .toList();

        Map<Long, DocumentChunk> chunkMap = documentChunkService.listByChunkIds(chunkIds)
                .stream()
                .collect(Collectors.toMap(DocumentChunk::getId, item -> item));

        List<RetrievedChunkVO> rawHitChunks = filteredVectorResults.stream()
                .map(item -> buildRetrievedChunkVO(item, chunkMap.get(item.getChunkId())))
                .filter(item -> item != null)
                .toList();

        // 1. 对向量直接命中结果做去重、低分过滤、单文档数量限制
        List<RetrievedChunkVO> filteredHitChunks = retrievalResultFilterService.filterHitChunks(
                rawHitChunks,
                scoreThreshold
        );

        // 2. 对过滤后的命中结果做相邻 chunk 扩展
        List<RetrievedChunkVO> expandedContextChunks = contextExpansionService.expand(filteredHitChunks);

        // 3. 对扩展后的上下文再次做去重和数量截断
        List<RetrievedChunkVO> finalContextChunks = retrievalResultFilterService.filterContextChunks(
                expandedContextChunks
        );

        RetrievalTestVO vo = new RetrievalTestVO();
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
        vo.setQuery(request.getQuery());
        vo.setTopK(topK);
        vo.setHitCount(filteredHitChunks.size());
        vo.setContextCount(finalContextChunks.size());
        vo.setResultCount(finalContextChunks.size());
        vo.setCostMs(System.currentTimeMillis() - start);
        vo.setResults(finalContextChunks);
        return vo;
    }

    private RetrievedChunkVO buildRetrievedChunkVO(VectorSearchResult result, DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }

        KnowledgeDocument document = knowledgeDocumentService.getById(chunk.getDocumentId());

        RetrievedChunkVO vo = new RetrievedChunkVO();
        vo.setChunkId(String.valueOf(chunk.getId()));
        vo.setDocumentId(String.valueOf(chunk.getDocumentId()));
        vo.setFileName(document == null ? null : document.getFileName());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setScore(result.getScore());
        vo.setHit(true);
        vo.setSourceChunkId(String.valueOf(chunk.getId()));
        vo.setDistance(0);
        vo.setChunkHash(chunk.getChunkHash());
        vo.setChunkText(chunk.getChunkText());
        vo.setTextPreview(buildPreview(chunk.getChunkText()));
        return vo;
    }

    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        int maxLength = 200;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}