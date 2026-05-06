package com.lablink.cloudmind.module.reranker.client;

import com.alibaba.fastjson2.JSON;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.reranker.config.RerankerProperties;
import com.lablink.cloudmind.module.reranker.dto.RerankRequest;
import com.lablink.cloudmind.module.reranker.dto.RerankResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 本地 BGE Reranker 客户端。
 */
@Slf4j
@Service
public class LocalBgeRerankerClient implements RerankerClient {

    private final RerankerProperties properties;

    private final HttpClient httpClient;

    public LocalBgeRerankerClient(RerankerProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public RerankResponse rerank(String query, List<String> documents, Integer topK) {
        validateConfig();

        if (!StringUtils.hasText(query) || CollectionUtils.isEmpty(documents)) {
            RerankResponse empty = new RerankResponse();
            empty.setModel(properties.getModel());
            empty.setCount(0);
            empty.setResults(List.of());
            return empty;
        }

        try {
            RerankRequest body = new RerankRequest();
            body.setQuery(query);
            body.setDocuments(documents);
            body.setTopK(topK);
            body.setBatchSize(properties.getBatchSize());

            String url = properties.getBaseUrl() + "/rerank";
            String jsonBody = JSON.toJSONString(body);

            log.info("调用本地 Reranker 服务：url={}, documentCount={}, topK={}",
                    url, documents.size(), topK);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.RERANKER_SERVICE_ERROR,
                        "调用本地 Reranker 服务失败：HTTP " + response.statusCode()
                                + "，响应：" + response.body()
                );
            }

            return JSON.parseObject(response.body(), RerankResponse.class);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.RERANKER_SERVICE_ERROR,
                    "调用本地 Reranker 服务失败：" + ex.getMessage()
            );
        }
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(ErrorCode.RERANKER_SERVICE_ERROR, "Reranker baseUrl 未配置");
        }
    }
}