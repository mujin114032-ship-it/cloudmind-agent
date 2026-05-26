package com.lablink.cloudmind.module.rag.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class RagTraceDetailVO extends RagTraceVO {

    private String promptVersion;

    private String systemPrompt;

    private String userPrompt;

    private String answer;

    private List<RagTraceChunkVO> chunks;
}