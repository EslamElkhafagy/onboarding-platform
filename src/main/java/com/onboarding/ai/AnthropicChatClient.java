package com.onboarding.ai;

import com.onboarding.config.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Grounded answers via the Anthropic Messages API (Claude). Defaults to claude-haiku-4-5 for
 * cost; override app.ai.anthropic.model to use Opus. The API key is read at call time so the
 * app boots without one — calls fail until ANTHROPIC_API_KEY is set.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "paid", matchIfMissing = true)
public class AnthropicChatClient implements ChatClient {

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final String anthropicVersion;
    private final int maxTokens;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;

    public AnthropicChatClient(
            @Value("${app.ai.anthropic.base-url:https://api.anthropic.com/v1}") String baseUrl,
            @Value("${app.ai.anthropic.api-key:${ANTHROPIC_API_KEY:}}") String apiKey,
            @Value("${app.ai.anthropic.model:claude-haiku-4-5}") String model,
            @Value("${app.ai.anthropic.version:2023-06-01}") String anthropicVersion,
            @Value("${app.ai.anthropic.max-tokens:1024}") int maxTokens,
            @Value("${app.ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${app.ai.retry.backoff-ms:500}") long retryBackoffMs) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.anthropicVersion = anthropicVersion;
        this.maxTokens = maxTokens;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNCONFIGURED",
                    "ANTHROPIC_API_KEY is not set");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)));

        Map<String, Object> response;
        try {
            response = AiRetry.withRetry(retryMaxAttempts, retryBackoffMs, () -> http.post()
                    .uri("/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .body(body)
                    .retrieve()
                    .body(Map.class));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_FAILED",
                    "LLM request failed: " + e.getMessage());
        }

        if (response == null || response.get("content") == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_FAILED", "Empty LLM response");
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        return content.stream()
                .filter(block -> "text".equals(block.get("type")))
                .map(block -> (String) block.get("text"))
                .reduce("", String::concat)
                .trim();
    }
}
