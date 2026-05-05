package com.lablink.cloudmind.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.util.HashUtils;
import com.lablink.cloudmind.module.knowledge.dto.DocumentChunkQueryRequest;
import com.lablink.cloudmind.module.knowledge.entity.DocumentChunk;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.mapper.DocumentChunkMapper;
import com.lablink.cloudmind.module.knowledge.service.DocumentChunkService;
import com.lablink.cloudmind.module.knowledge.vo.DocumentChunkVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk>
        implements DocumentChunkService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceDocumentChunks(KnowledgeDocument document, List<String> chunkTexts) {
        // 重新解析文档时，先逻辑删除旧分块，再写入新分块。
        LambdaQueryWrapper<DocumentChunk> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(DocumentChunk::getDocumentId, document.getId());
        this.remove(removeWrapper);

        List<DocumentChunk> chunks = new ArrayList<>();

        for (int i = 0; i < chunkTexts.size(); i++) {
            String chunkText = chunkTexts.get(i);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setDocumentId(document.getId());
            chunk.setUserId(document.getUserId());
            chunk.setChunkIndex(i);
            chunk.setChunkText(chunkText);
            chunk.setChunkHash(HashUtils.sha256Hex(chunkText));
            chunk.setCharCount(chunkText.length());

            // 第一阶段先粗略估算 token，后续接 tokenizer 后再精确计算。
            chunk.setTokenCount(chunkText.length());
            chunk.setEmbeddingModel(document.getEmbeddingModel());
            chunk.setEmbeddingDim(document.getEmbeddingDim());
            chunk.setEnabled(1);
            chunk.setDeleted(0);

            chunks.add(chunk);
        }

        this.saveBatch(chunks);
    }

    @Override
    public PageResult<DocumentChunkVO> pageChunks(Long documentId, DocumentChunkQueryRequest request) {
        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 20L : request.getPageSize();

        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex);

        Page<DocumentChunk> page = this.page(new Page<>(pageNo, pageSize), wrapper);

        List<DocumentChunkVO> records = page.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public List<DocumentChunk> listEnabledChunksByDocumentId(Long documentId) {
        return this.lambdaQuery()
                .eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getEnabled, 1)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markChunksVectorized(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        for (DocumentChunk chunk : chunks) {
            // 当前 Milvus 主键使用 chunk.id，因此 vector_id 直接记录 chunk.id。
            this.lambdaUpdate()
                    .eq(DocumentChunk::getId, chunk.getId())
                    .set(DocumentChunk::getVectorId, String.valueOf(chunk.getId()))
                    .update();
        }
    }

    @Override
    public List<DocumentChunk> listByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }

        return this.lambdaQuery()
                .in(DocumentChunk::getId, chunkIds)
                .eq(DocumentChunk::getEnabled, 1)
                .list();
    }

    @Override
    public List<DocumentChunk> listEnabledChunksByDocumentAndIndexRange(
            Long documentId,
            Integer startIndex,
            Integer endIndex
    ) {
        if (documentId == null || startIndex == null || endIndex == null) {
            return List.of();
        }

        return this.lambdaQuery()
                .eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getEnabled, 1)
                .ge(DocumentChunk::getChunkIndex, Math.max(startIndex, 0))
                .le(DocumentChunk::getChunkIndex, endIndex)
                .orderByAsc(DocumentChunk::getChunkIndex)
                .list();
    }

    private DocumentChunkVO convertToVO(DocumentChunk chunk) {
        DocumentChunkVO vo = new DocumentChunkVO();
        vo.setChunkId(String.valueOf(chunk.getId()));
        vo.setDocumentId(String.valueOf(chunk.getDocumentId()));
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setChunkText(chunk.getChunkText());
        vo.setChunkHash(chunk.getChunkHash());
        vo.setTokenCount(chunk.getTokenCount());
        vo.setCharCount(chunk.getCharCount());
        vo.setEmbeddingModel(chunk.getEmbeddingModel());
        vo.setEmbeddingDim(chunk.getEmbeddingDim());
        vo.setEnabled(chunk.getEnabled());
        return vo;
    }
}