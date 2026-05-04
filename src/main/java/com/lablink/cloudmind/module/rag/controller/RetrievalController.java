package com.lablink.cloudmind.module.rag.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.rag.application.RetrievalApplicationService;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestRequest;
import com.lablink.cloudmind.module.rag.dto.RetrievalTestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 检索测试接口。
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class RetrievalController {

    private final RetrievalApplicationService retrievalApplicationService;

    /**
     * 对指定知识库执行向量检索测试。
     */
    @PostMapping("/{knowledgeBaseId}/retrieval-test")
    public Result<RetrievalTestVO> retrievalTest(
            @PathVariable Long knowledgeBaseId,
            @Valid @RequestBody RetrievalTestRequest request
    ) {
        return Result.success(retrievalApplicationService.retrievalTest(knowledgeBaseId, request));
    }
}