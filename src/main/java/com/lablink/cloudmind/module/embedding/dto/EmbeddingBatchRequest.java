package com.lablink.cloudmind.module.embedding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 调用本地 Embedding 服务的批量请求体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingBatchRequest {

    private List<String> texts;

    /**
     * query：用户问题；passage：文档片段。
     */
    private String mode;

    private Boolean normalize;

    private Integer batchSize;
}
