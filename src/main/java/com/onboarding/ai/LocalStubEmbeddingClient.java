package com.onboarding.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline, $0 embeddings for the "free" provider mode (app.ai.provider=free). No network, no
 * keys: each text is turned into a deterministic bag-of-words vector by hashing its tokens into
 * the configured dimension and L2-normalising. This is keyword-overlap similarity, not semantic
 * — good enough to exercise the full ingest/retrieve pipeline locally, not for real answers.
 *
 * Produces exactly {@code dimension} components so it matches the documents schema vector(1536).
 * Embeddings are NOT comparable across providers: re-ingest documents after switching provider.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "free")
public class LocalStubEmbeddingClient implements EmbeddingClient {

    private final int dimension;

    public LocalStubEmbeddingClient(
            @Value("${app.ai.embeddings.dimension:1536}") int dimension) {
        this.dimension = dimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (String text : texts) {
            out.add(embedOne(text));
        }
        return out;
    }

    private float[] embedOne(String text) {
        float[] vec = new float[dimension];
        if (text == null || text.isBlank()) {
            return vec; // zero vector; cosine search simply won't match it
        }
        for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
            if (token.isEmpty()) continue;
            int h = token.hashCode();
            int bucket = Math.floorMod(h, dimension);
            // Sign from a separate bit so different tokens can cancel rather than only add.
            vec[bucket] += ((h & 1) == 0) ? 1f : -1f;
        }
        normalize(vec);
        return vec;
    }

    private void normalize(float[] vec) {
        double sumSq = 0;
        for (float v : vec) sumSq += (double) v * v;
        if (sumSq == 0) return;
        float norm = (float) Math.sqrt(sumSq);
        for (int i = 0; i < vec.length; i++) vec[i] /= norm;
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
