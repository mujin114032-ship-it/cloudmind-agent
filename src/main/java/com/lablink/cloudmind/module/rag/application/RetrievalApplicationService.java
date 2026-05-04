package com.lablink.cloudmind.module.rag.application;

import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.embedding.client.EmbeddingClient;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeBase;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
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
 * <p>负责串联：问题向量化 → Milvus 检索 → MySQL 回查 chunk 文本。</p>
 */
@Service
@RequiredArgsConstructor
public class RetrievalApplicationService {

    private final KnowledgeBaseService knowledgeBaseService;

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final EmbeddingClient embeddingClient;

    private final VectorStoreService vectorStoreService;

    public RetrievalTestVO retrievalTest(Long knowledgeBaseId, RetrievalTestRequest request) {
        long start = System.currentTimeMillis();

        Long userId = UserContext.getCurrentUserId();

        // 先校验知识库归属，避免跨用户检索。
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

        List<VectorSearchResult> filteredResults = vectorResults.stream()
                .filter(item -> item.getScore() != null && item.getScore() >= scoreThreshold)
                .toList();

        List<Long> chunkIds = filteredResults.stream()
                .map(VectorSearchResult::getChunkId)
                .toList();

        Map<Long, DocumentChunk> chunkMap = documentChunkService.listByChunkIds(chunkIds)
                .stream()
                .collect(Collectors.toMap(DocumentChunk::getId, item -> item));

        List<RetrievedChunkVO> results = filteredResults.stream()
                .map(item -> buildRetrievedChunkVO(item, chunkMap.get(item.getChunkId())))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(RetrievedChunkVO::getScore).reversed())
                .toList();

        RetrievalTestVO vo = new RetrievalTestVO();
        vo.setKnowledgeBaseId(String.valueOf(knowledgeBaseId));
        vo.setQuery(request.getQuery());
        vo.setTopK(topK);
        vo.setResultCount(results.size());
        vo.setCostMs(System.currentTimeMillis() - start);
        vo.setResults(results);
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