package com.lablink.cloudmind.module.reranker.client;

import com.lablink.cloudmind.module.reranker.dto.RerankResponse;

import java.util.List;

public interface RerankerClient {

    RerankResponse rerank(String query, List<String> documents, Integer topK);

    String modelName();
}