package com.lablink.cloudmind.module.embedding.client;

import com.alibaba.fastjson2.JSON;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.ai.config.AiServiceProperties;
import com.lablink.cloudmind.module.embedding.config.EmbeddingProperties;
import com.lablink.cloudmind.module.embedding.dto.EmbeddingBatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地 BGE Embedding 客户端。
 *
 * <p>Spring Boot 不直接加载模型，而是通过 HTTP 调用 FastAPI 模型服务。</p>
 */
@Slf4j
@Service
public class LocalBgeEmbeddingClient implements EmbeddingClient {

    private static final String MODE_QUERY = "query";

    private static final String MODE_PASSAGE = "passage";

    private final EmbeddingProperties properties;

    private final AiServiceProperties aiServiceProperties;

    private final HttpClient httpClient;

    public LocalBgeEmbeddingClient(
            EmbeddingProperties properties,
            AiServiceProperties aiServiceProperties
    ) {
        this.properties = properties;
        this.aiServiceProperties = aiServiceProperties;

        // Uvicorn/FastAPI 对 h2c upgrade 支持不好，这里强制使用 HTTP/1.1，避免请求体丢失。
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public float[] embedQuery(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "向量化文本不能为空");
        }

        List<float[]> vectors = callEmbedBatch(List.of(text), MODE_QUERY);
        return vectors.getFirst();
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (CollectionUtils.isEmpty(texts)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "向量化文本列表不能为空");
        }

        return callEmbedBatch(texts, MODE_PASSAGE);
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    @Override
    public int dimension() {
        return properties.getDimension();
    }

    private List<float[]> callEmbedBatch(List<String> texts, String mode) {
        List<float[]> result = new ArrayList<>();
        int batchSize = properties.getBatchSize();

        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> batchTexts = texts.subList(start, end);

            EmbeddingBatchResponse response = requestRemoteEmbedding(batchTexts, mode);
            result.addAll(convertAndValidate(response, batchTexts.size()));
        }

        return result;
    }

    private EmbeddingBatchResponse requestRemoteEmbedding(List<String> texts, String mode) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("texts", texts);
            requestBody.put("mode", mode);
            requestBody.put("normalize", true);
            requestBody.put("batchSize", properties.getBatchSize());

            String jsonBody = JSON.toJSONString(requestBody);
            String url = aiServiceProperties.getBaseUrl() + "/embedBatch";

            log.info("调用本地 Embedding 服务：url={}, mode={}, textCount={}, batchSize={}",
                    url,
                    mode,
                    texts.size(),
                    properties.getBatchSize());

            log.debug("Embedding 请求体预览：{}", jsonBody.length() > 500
                    ? jsonBody.substring(0, 500) + "..."
                    : jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode < 200 || statusCode >= 300) {
                throw new BusinessException(
                        ErrorCode.EMBEDDING_SERVICE_ERROR,
                        "调用本地 Embedding 服务失败：HTTP " + statusCode + "，响应：" + responseBody
                );
            }

            EmbeddingBatchResponse embeddingResponse = JSON.parseObject(
                    responseBody,
                    EmbeddingBatchResponse.class
            );

            if (embeddingResponse == null) {
                throw new BusinessException(ErrorCode.EMBEDDING_SERVICE_ERROR, "Embedding 服务返回为空");
            }

            return embeddingResponse;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.EMBEDDING_SERVICE_ERROR,
                    "调用本地 Embedding 服务失败：" + ex.getMessage()
            );
        }
    }

    private List<float[]> convertAndValidate(EmbeddingBatchResponse response, int expectedCount) {
        if (!properties.getDimension().equals(response.getDimension())) {
            throw new BusinessException(
                    ErrorCode.EMBEDDING_DIMENSION_MISMATCH,
                    "期望向量维度 " + properties.getDimension() + "，实际维度 " + response.getDimension()
            );
        }

        List<List<Double>> embeddings = response.getEmbeddings();
        if (embeddings == null || embeddings.size() != expectedCount) {
            throw new BusinessException(
                    ErrorCode.EMBEDDING_SERVICE_ERROR,
                    "Embedding 返回数量不匹配，期望 " + expectedCount + "，实际 "
                            + (embeddings == null ? 0 : embeddings.size())
            );
        }

        List<float[]> vectors = new ArrayList<>();

        for (List<Double> embedding : embeddings) {
            if (embedding.size() != properties.getDimension()) {
                throw new BusinessException(ErrorCode.EMBEDDING_DIMENSION_MISMATCH);
            }

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            vectors.add(vector);
        }

        return vectors;
    }
}
