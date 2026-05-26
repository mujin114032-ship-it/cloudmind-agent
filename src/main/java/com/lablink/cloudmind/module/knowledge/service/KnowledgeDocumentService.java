package com.lablink.cloudmind.module.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.knowledge.dto.AddKnowledgeDocumentRequest;
import com.lablink.cloudmind.module.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.lablink.cloudmind.module.knowledge.entity.KnowledgeDocument;
import com.lablink.cloudmind.module.knowledge.vo.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    KnowledgeDocumentVO addDocumentToKnowledgeBase(Long knowledgeBaseId, AddKnowledgeDocumentRequest request);

    PageResult<KnowledgeDocumentVO> pageDocuments(Long knowledgeBaseId, KnowledgeDocumentQueryRequest request);

    KnowledgeDocumentVO getDocumentDetail(Long documentId);

    void removeDocument(Long documentId);

    KnowledgeDocumentVO uploadDocumentToKnowledgeBase(Long knowledgeBaseId, MultipartFile file);

    KnowledgeDocument getCurrentUserDocumentEntity(Long documentId);

    void markParsing(Long documentId);

    void markParseSuccess(Long documentId, Integer chunkCount, String parserType);

    void markParseFailed(Long documentId, String errorMessage);

    void markIngesting(Long documentId);

    void markIngestSuccess(Long documentId);

    void markIngestFailed(Long documentId, String errorMessage);
}