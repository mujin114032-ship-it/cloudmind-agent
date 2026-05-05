package com.lablink.cloudmind.module.rag.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.common.result.PageResult;
import com.lablink.cloudmind.common.util.UserContext;
import com.lablink.cloudmind.module.rag.dto.RagTraceChunkVO;
import com.lablink.cloudmind.module.rag.dto.RagTraceDetailVO;
import com.lablink.cloudmind.module.rag.dto.RagTraceQueryRequest;
import com.lablink.cloudmind.module.rag.dto.RagTraceVO;
import com.lablink.cloudmind.module.rag.entity.RagTrace;
import com.lablink.cloudmind.module.rag.entity.RagTraceChunk;
import com.lablink.cloudmind.module.rag.enums.RagTraceStatusEnum;
import com.lablink.cloudmind.module.rag.mapper.RagTraceChunkMapper;
import com.lablink.cloudmind.module.rag.mapper.RagTraceMapper;
import com.lablink.cloudmind.module.rag.model.RetrievedChunkVO;
import com.lablink.cloudmind.module.rag.service.RagTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagTraceServiceImpl extends ServiceImpl<RagTraceMapper, RagTrace>
        implements RagTraceService {

    private static final String DEFAULT_PROMPT_VERSION = "basic-v1";

    private final RagTraceChunkMapper ragTraceChunkMapper;

    @Override
    public Long startTrace(
            Long knowledgeBaseId,
            Integer requestType,
            String originalQuestion,
            String finalQuestion,
            Integer topK,
            Double scoreThreshold
    ) {
        RagTrace trace = new RagTrace();
        trace.setUserId(UserContext.getCurrentUserId());
        trace.setKnowledgeBaseId(knowledgeBaseId);
        trace.setRequestType(requestType);
        trace.setOriginalQuestion(originalQuestion);
        trace.setFinalQuestion(finalQuestion);
        trace.setTopK(topK);
        trace.setScoreThreshold(scoreThreshold);
        trace.setResultCount(0);
        trace.setPromptVersion(DEFAULT_PROMPT_VERSION);
        trace.setRetrievalCostMs(0L);
        trace.setLlmCostMs(0L);
        trace.setTotalCostMs(0L);
        trace.setStatus(RagTraceStatusEnum.PROCESSING.getCode());
        trace.setDeleted(0);

        this.save(trace);
        return trace.getId();
    }

    @Override
    public void recordRetrieval(Long traceId, List<RetrievedChunkVO> chunks, Long retrievalCostMs) {
        int resultCount = chunks == null ? 0 : chunks.size();

        this.lambdaUpdate()
                .eq(RagTrace::getId, traceId)
                .set(RagTrace::getResultCount, resultCount)
                .set(RagTrace::getRetrievalCostMs, retrievalCostMs)
                .update();

        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<RagTraceChunk> traceChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunkVO chunk = chunks.get(i);

            RagTraceChunk traceChunk = new RagTraceChunk();
            traceChunk.setTraceId(traceId);
            traceChunk.setChunkId(Long.valueOf(chunk.getChunkId()));
            traceChunk.setDocumentId(Long.valueOf(chunk.getDocumentId()));
            traceChunk.setFileName(chunk.getFileName());
            traceChunk.setChunkIndex(chunk.getChunkIndex());
            traceChunk.setHit(Boolean.TRUE.equals(chunk.getHit()) ? 1 : 0);
            traceChunk.setSourceChunkId(
                    chunk.getSourceChunkId() == null ? null : Long.valueOf(chunk.getSourceChunkId())
            );
            traceChunk.setDistance(chunk.getDistance());
            traceChunk.setRankNo(i + 1);
            traceChunk.setScore(chunk.getScore());
            traceChunk.setTextPreview(chunk.getTextPreview());

            traceChunks.add(traceChunk);
        }

        // 召回结果数量通常较少，第一版直接逐条插入即可。
        traceChunks.forEach(ragTraceChunkMapper::insert);
    }

    @Override
    public void recordPrompt(Long traceId, String promptVersion, String systemPrompt, String userPrompt) {
        this.lambdaUpdate()
                .eq(RagTrace::getId, traceId)
                .set(RagTrace::getPromptVersion, StringUtils.hasText(promptVersion) ? promptVersion : DEFAULT_PROMPT_VERSION)
                .set(RagTrace::getSystemPrompt, systemPrompt)
                .set(RagTrace::getUserPrompt, userPrompt)
                .update();
    }

    @Override
    public void markSuccess(Long traceId, String modelName, String answer, Long llmCostMs, Long totalCostMs) {
        this.lambdaUpdate()
                .eq(RagTrace::getId, traceId)
                .set(RagTrace::getModelName, modelName)
                .set(RagTrace::getAnswer, answer)
                .set(RagTrace::getLlmCostMs, llmCostMs)
                .set(RagTrace::getTotalCostMs, totalCostMs)
                .set(RagTrace::getStatus, RagTraceStatusEnum.SUCCESS.getCode())
                .set(RagTrace::getErrorMessage, null)
                .update();
    }

    @Override
    public void markFailed(Long traceId, String errorMessage, Long totalCostMs) {
        this.lambdaUpdate()
                .eq(RagTrace::getId, traceId)
                .set(RagTrace::getTotalCostMs, totalCostMs)
                .set(RagTrace::getStatus, RagTraceStatusEnum.FAILED.getCode())
                .set(RagTrace::getErrorMessage, errorMessage)
                .update();
    }

    @Override
    public PageResult<RagTraceVO> pageTraces(RagTraceQueryRequest request) {
        Long pageNo = request.getPageNo() == null || request.getPageNo() <= 0 ? 1L : request.getPageNo();
        Long pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10L : request.getPageSize();

        Page<RagTrace> page = this.lambdaQuery()
                .eq(RagTrace::getUserId, UserContext.getCurrentUserId())
                .eq(request.getKnowledgeBaseId() != null, RagTrace::getKnowledgeBaseId, request.getKnowledgeBaseId())
                .eq(request.getStatus() != null, RagTrace::getStatus, request.getStatus())
                .like(StringUtils.hasText(request.getKeyword()), RagTrace::getOriginalQuestion, request.getKeyword())
                .orderByDesc(RagTrace::getCreateTime)
                .page(new Page<>(pageNo, pageSize));

        List<RagTraceVO> records = page.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Override
    public RagTraceDetailVO getTraceDetail(Long traceId) {
        RagTrace trace = this.getById(traceId);
        if (trace == null || !UserContext.getCurrentUserId().equals(trace.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "RAG Trace 不存在");
        }

        RagTraceDetailVO vo = new RagTraceDetailVO();
        fillBaseVO(trace, vo);

        vo.setPromptVersion(trace.getPromptVersion());
        vo.setSystemPrompt(trace.getSystemPrompt());
        vo.setUserPrompt(trace.getUserPrompt());
        vo.setAnswer(trace.getAnswer());

        List<RagTraceChunkVO> chunks = ragTraceChunkMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagTraceChunk>()
                                .eq(RagTraceChunk::getTraceId, traceId)
                                .orderByAsc(RagTraceChunk::getRankNo)
                )
                .stream()
                .map(this::convertChunkToVO)
                .toList();

        vo.setChunks(chunks);
        return vo;
    }

    private RagTraceVO convertToVO(RagTrace trace) {
        RagTraceVO vo = new RagTraceVO();
        fillBaseVO(trace, vo);
        return vo;
    }

    private void fillBaseVO(RagTrace trace, RagTraceVO vo) {
        vo.setTraceId(String.valueOf(trace.getId()));
        vo.setKnowledgeBaseId(String.valueOf(trace.getKnowledgeBaseId()));
        vo.setRequestType(trace.getRequestType());
        vo.setOriginalQuestion(trace.getOriginalQuestion());
        vo.setFinalQuestion(trace.getFinalQuestion());
        vo.setTopK(trace.getTopK());
        vo.setScoreThreshold(trace.getScoreThreshold());
        vo.setResultCount(trace.getResultCount());
        vo.setModelName(trace.getModelName());
        vo.setRetrievalCostMs(trace.getRetrievalCostMs());
        vo.setLlmCostMs(trace.getLlmCostMs());
        vo.setTotalCostMs(trace.getTotalCostMs());
        vo.setStatus(trace.getStatus());
        vo.setErrorMessage(trace.getErrorMessage());
        vo.setCreateTime(trace.getCreateTime());
    }

    private RagTraceChunkVO convertChunkToVO(RagTraceChunk chunk) {
        RagTraceChunkVO vo = new RagTraceChunkVO();
        vo.setChunkId(String.valueOf(chunk.getChunkId()));
        vo.setDocumentId(String.valueOf(chunk.getDocumentId()));
        vo.setFileName(chunk.getFileName());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setHit(chunk.getHit() != null && chunk.getHit() == 1);
        vo.setSourceChunkId(chunk.getSourceChunkId() == null ? null : String.valueOf(chunk.getSourceChunkId()));
        vo.setDistance(chunk.getDistance());
        vo.setRankNo(chunk.getRankNo());
        vo.setScore(chunk.getScore());
        vo.setTextPreview(chunk.getTextPreview());
        return vo;
    }
}