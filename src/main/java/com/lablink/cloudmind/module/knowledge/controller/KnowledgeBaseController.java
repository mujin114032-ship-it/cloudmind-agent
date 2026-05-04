package com.lablink.cloudmind.module.knowledge.controller;

import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.knowledge.dto.CreateKnowledgeBaseRequest;
import com.lablink.cloudmind.module.knowledge.dto.KnowledgeBaseQueryRequest;
import com.lablink.cloudmind.module.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.lablink.cloudmind.module.knowledge.service.KnowledgeBaseService;
import com.lablink.cloudmind.module.knowledge.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public Result<KnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseService.createKnowledgeBase(request));
    }

    @GetMapping
    public Result<PageResult<KnowledgeBaseVO>> pageKnowledgeBases(KnowledgeBaseQueryRequest request) {
        return Result.success(knowledgeBaseService.pageKnowledgeBases(request));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getKnowledgeBaseDetail(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.getKnowledgeBaseDetail(id));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request
    ) {
        return Result.success(knowledgeBaseService.updateKnowledgeBase(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success();
    }
}