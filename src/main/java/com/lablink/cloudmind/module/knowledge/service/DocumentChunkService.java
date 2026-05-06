package com.lablink.cloudmind.module.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.knowledge.dto.DocumentChunkQueryRequest;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.vo.DocumentChunkVO;

import java.util.List;

public interface DocumentChunkService extends IService<DocumentChunk> {

    void replaceDocumentChunks(KnowledgeDocument document, List<String> chunkTexts);

    PageResult<DocumentChunkVO> pageChunks(Long documentId, DocumentChunkQueryRequest request);

    List<DocumentChunk> listEnabledChunksByDocumentId(Long documentId);

    void markChunksVectorized(List<DocumentChunk> chunks);

    List<DocumentChunk> listByChunkIds(List<Long> chunkIds);

    List<DocumentChunk> listEnabledChunksByDocumentAndIndexRange(
            Long documentId,
            Integer startIndex,
            Integer endIndex
    );

    List<DocumentChunk> searchByKeywords(
            Long knowledgeBaseId,
            Long userId,
            List<String> keywords,
            Integer limit
    );
}