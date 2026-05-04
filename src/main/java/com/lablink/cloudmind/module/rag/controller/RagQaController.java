package com.lablink.cloudmind.module.rag.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.rag.application.RagQaApplicationService;
import com.lablink.cloudmind.module.rag.dto.RagQaRequest;
import com.lablink.cloudmind.module.rag.dto.RagQaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 普通 RAG 问答接口。
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class RagQaController {

    private final RagQaApplicationService ragQaApplicationService;

    @PostMapping("/{knowledgeBaseId}/rag/qa")
    public Result<RagQaVO> qa(
            @PathVariable Long knowledgeBaseId,
            @Valid @RequestBody RagQaRequest request
    ) {
        return Result.success(ragQaApplicationService.qa(knowledgeBaseId, request));
    }
}