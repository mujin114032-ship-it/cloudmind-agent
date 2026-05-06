package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.embedding.client.EmbeddingClient;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.rag.config.RagProperties;
import com.lablink.cloudmind.module.rag.config.RagSearchOptions;
import com.lablink.cloudmind.module.rag.config.RagSearchOptionsResolver;
import com.lablink.cloudmind.module.rag.context.ContextExpansionService;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import com.lablink.cloudmind.module.rag.filter.RetrievalResultFilterService;
import com.lablink.cloudmind.module.rag.keyword.KeywordSearchResult;
import com.lablink.cloudmind.module.rag.keyword.KeywordSearchService;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.reranker.client.RerankerClient;
import com.lablink.cloudmind.module.reranker.dto.RerankResponse;
import com.lablink.cloudmind.module.vector.model.VectorSearchResult;
import com.lablink.cloudmind.module.vector.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索应用服务。
 *
 * <p>负责串联：问题向量化 → Milvus topK → 基础分数过滤 → 命中 chunk 去重 → 限制单文档命中数量 → 相邻 Chunk 扩展 → 扩展结果再次去重 → 限制最终上下文数量 → Prompt。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalApplicationService {

    private final KnowledgeBaseService knowledgeBaseService;

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final ContextExpansionService contextExpansionService;

    private final RetrievalResultFilterService retrievalResultFilterService;

    private final EmbeddingClient embeddingClient;

    private final RagProperties ragProperties;

    private final RagSearchOptionsResolver ragSearchOptionsResolver;

    private final RerankerClient rerankerClient;

    private final VectorStoreService vectorStoreService;

    private final KeywordSearchService keywordSearchService;

    public RetrievalTestVO retrievalTest(Long knowledgeBaseId, RetrievalTestRequest request) {
        long start = System.currentTimeMillis();

        Long userId = UserContext.getCurrentUserId();

        KnowledgeBase knowledgeBase = knowledgeBaseService.getCurrentUserKnowledgeBase(knowledgeBaseId);

        RagSearchOptions options = ragSearchOptionsResolver.resolve(
                request.getSearchMode(),
                request.getTopK(),
                request.getScoreThreshold()
        );

        int topK = options.getTopK();
        double scoreThreshold = options.getScoreThreshold();

        int candidateTopK = Boolean.TRUE.equals(options.getRerankEnabled())
                ? Math.max(topK, options.getVectorCandidateTopK())
                : topK;

        // 1. 向量召回
        float[] queryVector = embeddingClient.embedQuery(request.getQuery());

        List<VectorSearchResult> vectorResults = vectorStoreService.search(
                knowledgeBase.getId(),
                userId,
                queryVector,
                candidateTopK
        );

        // 2. 向量候选过滤：scoreThreshold 只在这里使用一次
        List<VectorSearchResult> filteredVectorResults = vectorResults.stream()
                .filter(item -> item.getScore() != null && item.getScore() >= scoreThreshold)
                .toList();

        // 3. 关键词召回
        List<KeywordSearchResult> keywordResults = Boolean.TRUE.equals(options.getKeywordEnabled())
                ? keywordSearchService.search(
                knowledgeBase.getId(),
                userId,
                request.getQuery(),
                options.getKeywordCandidateTopK()
        )
                : List.of();

        // 4. 合并向量候选 + 关键词候选
        List<RetrievedChunkVO> rawHitChunks = buildHybridCandidates(
                filteredVectorResults,
                keywordResults
        );

        // 5. Reranker 对混合候选统一重排
        List<RetrievedChunkVO> rerankedHitChunks = applyRerank(
                request.getQuery(),
                rawHitChunks,
                topK,
                options
        );

        // 6. 这里只做去重、单文档限制，不再按 scoreThreshold 过滤
        List<RetrievedChunkVO> filteredHitChunks = retrievalResultFilterService.filterHitChunks(
                rerankedHitChunks,
                options
        );

        // 7. 相邻 Chunk 扩展
        List<RetrievedChunkVO> expandedContextChunks = contextExpansionService.expand(
                filteredHitChunks,
                options
        );

        // 8. 扩展结果最终去重与截断
        List<RetrievedChunkVO> finalContextChunks = retrievalResultFilterService.filterContextChunks(
                expandedContextChunks,
                options
        );

        for (int i = 0; i < finalContextChunks.size(); i++) {
            finalContextChunks.get(i).setContextOrder(i + 1);
        }

        RetrievalTestVO vo = new RetrievalTestVO();
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
        vo.setQuery(request.getQuery());
        vo.setTopK(topK);
        vo.setHitCount(filteredHitChunks.size());
        vo.setContextCount(finalContextChunks.size());
        vo.setResultCount(finalContextChunks.size());
        vo.setCostMs(System.currentTimeMillis() - start);
        vo.setResults(finalContextChunks);
        vo.setSearchMode(request.getSearchMode());
        return vo;
    }

    private List<RetrievedChunkVO> buildHybridCandidates(
            List<VectorSearchResult> vectorResults,
            List<KeywordSearchResult> keywordResults
    ) {
        Map<Long, VectorSearchResult> vectorResultMap = vectorResults == null
                ? Map.of()
                : vectorResults.stream()
                .collect(Collectors.toMap(
                        VectorSearchResult::getChunkId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<Long, KeywordSearchResult> keywordResultMap = keywordResults == null
                ? Map.of()
                : keywordResults.stream()
                .collect(Collectors.toMap(
                        KeywordSearchResult::getChunkId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Set<Long> allChunkIds = new LinkedHashSet<>();
        allChunkIds.addAll(vectorResultMap.keySet());
        allChunkIds.addAll(keywordResultMap.keySet());

        if (allChunkIds.isEmpty()) {
            return List.of();
        }

        Map<Long, DocumentChunk> chunkMap = documentChunkService.listByChunkIds(new ArrayList<>(allChunkIds))
                .stream()
                .collect(Collectors.toMap(DocumentChunk::getId, item -> item));

        List<RetrievedChunkVO> candidates = new ArrayList<>();

        for (Long chunkId : allChunkIds) {
            DocumentChunk chunk = chunkMap.get(chunkId);
            if (chunk == null) {
                continue;
            }

            VectorSearchResult vectorResult = vectorResultMap.get(chunkId);
            KeywordSearchResult keywordResult = keywordResultMap.get(chunkId);

            RetrievedChunkVO vo = buildRetrievedChunkVO(vectorResult, chunk);
            if (vo == null) {
                continue;
            }

            if (keywordResult != null) {
                vo.setKeywordScore(keywordResult.getKeywordScore());
            }

            if (vectorResult != null && keywordResult != null) {
                vo.setRecallSource("hybrid");
            } else if (vectorResult != null) {
                vo.setRecallSource("vector");
            } else {
                vo.setRecallSource("keyword");
            }

            candidates.add(vo);
        }

        return candidates;
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

        // keyword-only 候选没有向量分数
        vo.setScore(result == null ? null : result.getScore());

        vo.setHit(true);
        vo.setSourceChunkId(String.valueOf(chunk.getId()));
        vo.setDistance(0);
        vo.setChunkHash(chunk.getChunkHash());
        vo.setChunkText(chunk.getChunkText());
        vo.setTextPreview(buildPreview(chunk.getChunkText()));
        return vo;
    }

    private List<RetrievedChunkVO> applyRerank(
            String query,
            List<RetrievedChunkVO> rawHitChunks,
            Integer topK,
            RagSearchOptions options
    ) {
        if (!Boolean.TRUE.equals(options.getRerankEnabled())) {
            return rawHitChunks.stream()
                    .limit(topK)
                    .toList();
        }

        if (rawHitChunks == null || rawHitChunks.isEmpty()) {
            return List.of();
        }

        List<String> documents = rawHitChunks.stream()
                .map(RetrievedChunkVO::getChunkText)
                .toList();

        RerankResponse rerankResponse = rerankerClient.rerank(query, documents, topK);

        if (rerankResponse == null || rerankResponse.getResults() == null || rerankResponse.getResults().isEmpty()) {
            return rawHitChunks.stream()
                    .limit(topK)
                    .toList();
        }

        List<RetrievedChunkVO> reranked = new ArrayList<>();
        int rank = 1;

        for (RerankResponse.RerankItem item : rerankResponse.getResults()) {
            Integer index = item.getIndex();

            if (index == null || index < 0 || index >= rawHitChunks.size()) {
                continue;
            }

            RetrievedChunkVO chunk = rawHitChunks.get(index);
            chunk.setRerankScore(item.getScore());
            chunk.setRerankRank(rank++);
            reranked.add(chunk);
        }

        return reranked;
    }

    private String buildPreview(String text) {
        if (text == null) {
            return "";
        }

        int maxLength = 200;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}