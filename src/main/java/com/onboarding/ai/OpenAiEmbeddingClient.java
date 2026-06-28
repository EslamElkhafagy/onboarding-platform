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
 * Embeddings via the OpenAI REST API (text-embedding-3-small, 1536 dims). Chosen to match
 * the documents schema's vector(1536); swap the model/dimension together if this changes.
 *
 * The API key is read from config at call time, so the app still boots without one — calls
 * just fail until OPENAI_API_KEY is set.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "paid", matchIfMissing = true)
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;

    public OpenAiEmbeddingClient(
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${app.ai.embeddings.model:text-embedding-3-small}") String model,
            @Value("${app.ai.embeddings.dimension:1536}") int dimension,
            @Value("${app.ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${app.ai.retry.backoff-ms:500}") long retryBackoffMs) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMBEDDINGS_UNCONFIGURED",
                    "OPENAI_API_KEY is not set");
        }

        Map<String, Object> body = Map.of("model", model, "input", texts);
        Map<String, Object> response;
        try {
            response = AiRetry.withRetry(retryMaxAttempts, retryBackoffMs, () -> http.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMBEDDINGS_FAILED",
                    "Embedding request failed: " + e.getMessage());
        }

        if (response == null || response.get("data") == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMBEDDINGS_FAILED",
                    "Empty embedding response");
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data.stream().map(item -> {
            List<Number> vec = (List<Number>) item.get("embedding");
            float[] out = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) {
                out[i] = vec.get(i).floatValue();
            }
            return out;
        }).toList();
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
