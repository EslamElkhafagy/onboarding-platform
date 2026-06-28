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
 * Grounded answers via a local Ollama server (app.ai.provider=ollama) — free to run, no API key.
 * Requires Ollama installed and the chat model pulled (e.g. `ollama pull llama3.1`). Uses the
 * non-streaming /api/chat endpoint and reuses the shared retry/backoff for transient failures.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama")
public class OllamaChatClient implements ChatClient {

    private final RestClient http;
    private final String model;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;

    public OllamaChatClient(
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.chat-model:llama3.1}") String model,
            @Value("${app.ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${app.ai.retry.backoff-ms:500}") long retryBackoffMs) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        Map<String, Object> response;
        try {
            response = AiRetry.withRetry(retryMaxAttempts, retryBackoffMs, () -> http.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(Map.class));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_FAILED",
                    "Ollama chat request failed (is Ollama running?): " + e.getMessage());
        }

        Object message = response == null ? null : response.get("message");
        if (!(message instanceof Map)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_FAILED", "Empty Ollama response");
        }
        Object content = ((Map<String, Object>) message).get("content");
        return content == null ? "" : content.toString().trim();
    }
}
