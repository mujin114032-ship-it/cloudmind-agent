package com.lablink.cloudmind.module.embedding.application;

import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.embedding.client.EmbeddingClient;
import com.lablink.cloudmind.module.embedding.dto.DocumentIngestVO;
import com.lablink.cloudmind.module.embedding.dto.EmbeddingTestVO;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.enums.IngestStatusEnum;
import com.lablink.cloudmind.module.knowledge.enums.ParseStatusEnum;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeDocumentService;
import com.lablink.cloudmind.module.vector.model.ChunkVectorRow;
import com.lablink.cloudmind.module.vector.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档向量化应用服务。
 *
 * <p>当前阶段只验证 chunk 是否能成功生成向量，不负责写入 Milvus。</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingApplicationService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final EmbeddingClient embeddingClient;

    private final VectorStoreService vectorStoreService;

    public DocumentIngestVO ingestDocumentChunks(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getCurrentUserDocumentEntity(documentId);

        if (!ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }

        List<DocumentChunk> chunks = documentChunkService.listEnabledChunksByDocumentId(documentId);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_CHUNK_NOT_FOUND);
        }

        knowledgeDocumentService.markIngesting(documentId);

        long start = System.currentTimeMillis();

        try {
            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getChunkText)
                    .toList();

            List<float[]> vectors = embeddingClient.embedDocuments(texts);
            validateVectors(vectors, chunks.size());

            List<ChunkVectorRow> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);

                ChunkVectorRow row = new ChunkVectorRow();
                row.setChunkId(chunk.getId());
                row.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
                row.setDocumentId(chunk.getDocumentId());
                row.setUserId(chunk.getUserId());
                row.setChunkIndex(chunk.getChunkIndex());
                row.setEmbedding(vectors.get(i));

                rows.add(row);
            }

            vectorStoreService.insertChunkVectors(rows);
            documentChunkService.markChunksVectorized(chunks);
            knowledgeDocumentService.markIngestSuccess(documentId);

            long costMs = System.currentTimeMillis() - start;

            DocumentIngestVO vo = new DocumentIngestVO();
            vo.setDocumentId(String.valueOf(documentId));
            vo.setChunkCount(chunks.size());
            vo.setVectorCount(vectors.size());
            vo.setModelName(embeddingClient.modelName());
            vo.setEmbeddingDim(embeddingClient.dimension());
            vo.setCostMs(costMs);
            vo.setIngestStatus(IngestStatusEnum.SUCCESS.getCode());
            vo.setSuccess(true);
            return vo;
        } catch (Exception ex) {
            knowledgeDocumentService.markIngestFailed(documentId, ex.getMessage());
            throw ex;
        }
    }

    // 测试用，不负责写入 Milvus
    public EmbeddingTestVO embedDocumentChunks(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentService.getCurrentUserDocumentEntity(documentId);

        if (!ParseStatusEnum.SUCCESS.getCode().equals(document.getParseStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }

        List<DocumentChunk> chunks = documentChunkService.listEnabledChunksByDocumentId(documentId);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_CHUNK_NOT_FOUND);
        }

        List<String> texts = chunks.stream()
                .map(DocumentChunk::getChunkText)
                .toList();

        long start = System.currentTimeMillis();
        List<float[]> vectors = embeddingClient.embedDocuments(texts);
        long costMs = System.currentTimeMillis() - start;

        validateVectors(vectors, chunks.size());

        EmbeddingTestVO vo = new EmbeddingTestVO();
        vo.setDocumentId(String.valueOf(documentId));
        vo.setChunkCount(chunks.size());
        vo.setVectorCount(vectors.size());
        vo.setModelName(embeddingClient.modelName());
        vo.setEmbeddingDim(embeddingClient.dimension());
        vo.setCostMs(costMs);
        vo.setSuccess(true);
        return vo;
    }

    private void validateVectors(List<float[]> vectors, int expectedCount) {
        if (vectors == null || vectors.size() != expectedCount) {
            throw new BusinessException(ErrorCode.EMBEDDING_SERVICE_ERROR, "向量数量与分块数量不一致");
        }

        for (float[] vector : vectors) {
            if (vector == null || vector.length != embeddingClient.dimension()) {
                throw new BusinessException(ErrorCode.EMBEDDING_DIMENSION_MISMATCH);
            }
        }
    }
}