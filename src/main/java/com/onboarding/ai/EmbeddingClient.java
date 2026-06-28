package com.onboarding.ai;

import java.util.List;

/**
 * Turns text into embedding vectors. Implementations talk to a specific provider; the rest
 * of the app depends only on this interface so the provider/model can be swapped without
 * touching ingestion or retrieval.
 *
 * The vector dimension MUST match the documents schema (vector(1536)).
 */
public interface EmbeddingClient {

    /** Embeds a batch of texts, preserving order. */
    List<float[]> embed(List<String> texts);

    /** Embeds a single text (e.g. a search query). */
    default float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }

    /** The dimension of the vectors this client produces. */
    int dimension();
}
