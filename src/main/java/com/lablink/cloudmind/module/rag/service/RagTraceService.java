package com.lablink.cloudmind.module.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.module.rag.dto.RagTraceDetailVO;
import com.lablink.cloudmind.module.rag.dto.RagTraceQueryRequest;
import com.lablink.cloudmind.module.rag.dto.RagTraceVO;
import com.lablink.cloudmind.module.rag.entity.RagTrace;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;

import java.util.List;

public interface RagTraceService extends IService<RagTrace> {

    Long startTrace(
            Long knowledgeBaseId,
            Integer requestType,
            String originalQuestion,
            String finalQuestion,
            Integer topK,
            Double scoreThreshold
    );

    void recordRetrieval(Long traceId, List<RetrievedChunkVO> chunks, Long retrievalCostMs);

    void recordPrompt(Long traceId, String promptVersion, String systemPrompt, String userPrompt);

    void markSuccess(Long traceId, String modelName, String answer, Long llmCostMs, Long totalCostMs);

    void markFailed(Long traceId, String errorMessage, Long totalCostMs);

    PageResult<RagTraceVO> pageTraces(RagTraceQueryRequest request);

    RagTraceDetailVO getTraceDetail(Long traceId);
}