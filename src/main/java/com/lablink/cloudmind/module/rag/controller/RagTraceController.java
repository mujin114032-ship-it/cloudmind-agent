package com.lablink.cloudmind.module.rag.controller;

import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.result.Result;
import com.lablink.cloudmind.module.rag.dto.RagTraceDetailVO;
import com.lablink.cloudmind.module.rag.dto.RagTraceQueryRequest;
import com.lablink.cloudmind.module.rag.dto.RagTraceVO;
import com.lablink.cloudmind.module.rag.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG Trace 查询接口。
 */
@RestController
@RequestMapping("/api/rag/traces")
@RequiredArgsConstructor
public class RagTraceController {

    private final RagTraceService ragTraceService;

    @GetMapping
    public Result<PageResult<RagTraceVO>> pageTraces(RagTraceQueryRequest request) {
        return Result.success(ragTraceService.pageTraces(request));
    }

    @GetMapping("/{traceId}")
    public Result<RagTraceDetailVO> getTraceDetail(@PathVariable Long traceId) {
        return Result.success(ragTraceService.getTraceDetail(traceId));
    }
}