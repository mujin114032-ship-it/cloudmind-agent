package com.lablink.cloudmind.module.rag.controller;

import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.rag.dto.PromptTemplateVO;
import com.lablink.cloudmind.module.rag.prompt.RagPromptTemplate;
import com.lablink.cloudmind.module.rag.prompt.RagPromptTemplateRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG Prompt 模板接口。
 */
@RestController
@RequiredArgsConstructor
public class RagPromptController {

    private final RagPromptTemplateRegistry templateRegistry;

    @GetMapping("/api/rag/prompt-templates")
    public Result<List<PromptTemplateVO>> listPromptTemplates() {
        List<PromptTemplateVO> list = templateRegistry.listTemplates()
                .stream()
                .map(this::convertToVO)
                .toList();

        return Result.success(list);
    }

    private PromptTemplateVO convertToVO(RagPromptTemplate template) {
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setVersion(template.getVersion());
        vo.setName(template.getName());
        return vo;
    }
}