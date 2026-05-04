package com.lablink.cloudmind.module.llm.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lablink.cloudmind.common.enums.ErrorCode;
import com.lablink.cloudmind.common.exception.BusinessException;
import com.lablink.cloudmind.module.llm.config.LlmProperties;
import com.lablink.cloudmind.module.llm.dto.ChatCompletionRequest;
import com.lablink.cloudmind.module.llm.dto.ChatCompletionResponse;
import com.lablink.cloudmind.module.llm.dto.ChatMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * OpenAI-compatible 大模型客户端。
 *
 * <p>使用 JDK HttpClient 显式发送 JSON，避免不同 Spring HTTP 客户端版本带来的适配问题。</p>
 */
@Slf4j
@Service
public class OpenAiCompatibleChatClient implements ChatClient {

    private final LlmProperties properties;

    private final HttpClient httpClient;

    public OpenAiCompatibleChatClient(LlmProperties properties) {
        this.properties = properties;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        validateConfig();

        try {
            ChatCompletionRequest requestBody = new ChatCompletionRequest();
            requestBody.setModel(properties.getModel());
            requestBody.setTemperature(0.2);
            requestBody.setStream(false);
            requestBody.setMessages(List.of(
                    new ChatMessageDTO("system", systemPrompt),
                    new ChatMessageDTO("user", userPrompt)
            ));

            String jsonBody = JSON.toJSONString(requestBody);
            String url = properties.getBaseUrl() + "/chat/completions";

            log.info("调用大模型：url={}, model={}", url, properties.getModel());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.LLM_SERVICE_ERROR,
                        "大模型调用失败：HTTP " + response.statusCode() + "，响应：" + response.body()
                );
            }

            ChatCompletionResponse completionResponse = JSON.parseObject(
                    response.body(),
                    ChatCompletionResponse.class
            );

            return extractAnswer(completionResponse);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.LLM_SERVICE_ERROR,
                    "大模型调用失败：" + ex.getMessage()
            );
        }
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        validateConfig();

        try {
            ChatCompletionRequest requestBody = new ChatCompletionRequest();
            requestBody.setModel(properties.getModel());
            requestBody.setTemperature(0.2);
            requestBody.setStream(true);
            requestBody.setMessages(List.of(
                    new ChatMessageDTO("system", systemPrompt),
                    new ChatMessageDTO("user", userPrompt)
            ));

            String jsonBody = JSON.toJSONString(requestBody);
            String url = properties.getBaseUrl() + "/chat/completions";

            log.info("流式调用大模型：url={}, model={}", url, properties.getModel());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofLines()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = response.body().limit(20).reduce("", (a, b) -> a + b);
                throw new BusinessException(
                        ErrorCode.LLM_SERVICE_ERROR,
                        "大模型流式调用失败：HTTP " + response.statusCode() + "，响应：" + errorBody
                );
            }

            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleStreamLine(line, onDelta));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.LLM_SERVICE_ERROR,
                    "大模型流式调用失败：" + ex.getMessage()
            );
        }
    }

    private void handleStreamLine(String line, Consumer<String> onDelta) {
        if (!StringUtils.hasText(line)) {
            return;
        }

        String trimmed = line.trim();

        if (!trimmed.startsWith("data:")) {
            return;
        }

        String data = trimmed.substring("data:".length()).trim();

        if ("[DONE]".equals(data)) {
            return;
        }

        String delta = extractDeltaContent(data);
        if (StringUtils.hasText(delta)) {
            onDelta.accept(delta);
        }
    }

    private String extractDeltaContent(String data) {
        try {
            JSONObject jsonObject = JSON.parseObject(data);
            JSONArray choices = jsonObject.getJSONArray("choices");

            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.getJSONObject("delta");

            if (delta == null) {
                return null;
            }

            return delta.getString("content");
        } catch (Exception ex) {
            log.debug("忽略无法解析的大模型流式片段：{}", data);
            return null;
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "大模型 baseUrl 未配置");
        }

        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "大模型 apiKey 未配置");
        }

        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "大模型 model 未配置");
        }
    }

    private String extractAnswer(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "大模型返回结果为空");
        }

        ChatMessageDTO message = response.getChoices().getFirst().getMessage();
        if (message == null || !StringUtils.hasText(message.getContent())) {
            throw new BusinessException(ErrorCode.LLM_SERVICE_ERROR, "大模型回答内容为空");
        }

        return message.getContent();
    }
}