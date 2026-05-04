package com.lablink.cloudmind.module.embedding.client;

import java.util.List;

public interface EmbeddingClient {

    /**
     * 对用户问题生成向量。
     */
    float[] embedQuery(String text);

    /**
     * 对文档片段批量生成向量。
     */
    List<float[]> embedDocuments(List<String> texts);

    String modelName();

    int dimension();
}