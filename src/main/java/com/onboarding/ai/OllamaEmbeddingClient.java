package com.onboarding.ai;

import com.onboarding.config.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Embeddings via a local Ollama server (app.ai.provider=ollama) — free to run, no API key.
 * Requires Ollama installed and the embedding model pulled (e.g. `ollama pull nomic-embed-text`).
 *
 * Ollama embedding models rarely produce 1536 dims (nomic-embed-text is 768), but the documents
 * schema column is vector(1536). We zero-pad (or truncate) each vector to {@code dimension};
 * padding with zeros leaves cosine similarity unchanged as long as queries are padded the same
 * way (they are, since they go through this same client), so no schema migration is needed.
 *
 * Embeddings are NOT comparable across providers: re-ingest documents after switching provider.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama")
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient http;
    private final String model;
    private final int dimension;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;

    public OllamaEmbeddingClient(
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.embedding-model:nomic-embed-text}") String model,
            @Value("${app.ai.embeddings.dimension:1536}") int dimension,
            @Value("${app.ai.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${app.ai.retry.backoff-ms:500}") long retryBackoffMs) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
        this.dimension = dimension;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (String text : texts) {
            out.add(fitToDimension(embedOne(text)));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private float[] embedOne(String text) {
        Map<String, Object> body = Map.of("model", model, "prompt", text == null ? "" : text);
        Map<String, Object> response;
        try {
            response = AiRetry.withRetry(retryMaxAttempts, retryBackoffMs, () -> http.post()
                    .uri("/api/embeddings")
                    .body(body)
                    .retrieve()
                    .body(Map.class));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMBEDDINGS_FAILED",
                    "Ollama embedding request failed (is Ollama running?): " + e.getMessage());
        }
        Object embedding = response == null ? null : response.get("embedding");
        if (!(embedding instanceof List)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMBEDDINGS_FAILED",
                    "Empty Ollama embedding response");
        }
        List<Number> vec = (List<Number>) embedding;
        float[] out = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++) out[i] = vec.get(i).floatValue();
        return out;
    }

    /** Zero-pads or truncates to the schema dimension so the vector(1536) column accepts it. */
    private float[] fitToDimension(float[] vec) {
        if (vec.length == dimension) return vec;
        float[] fitted = new float[dimension];
        System.arraycopy(vec, 0, fitted, 0, Math.min(vec.length, dimension));
        return fitted;
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
