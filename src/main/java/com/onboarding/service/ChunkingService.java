package com.onboarding.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted text into overlapping, roughly-token-sized chunks for embedding.
 *
 * Tokens are estimated from word count (~1.33 tokens/word for English) rather than run
 * through a real tokenizer — good enough to keep chunks comfortably under model limits,
 * and avoids a heavy tokenizer dependency. The trailing overlap preserves context across
 * chunk boundaries so retrieval doesn't split an answer in half.
 */
@Service
public class ChunkingService {

    private static final int TARGET_TOKENS = 500;
    private static final int OVERLAP_TOKENS = 50;
    private static final double TOKENS_PER_WORD = 1.33;

    public record Chunk(String content, int tokenCount) {}

    public List<Chunk> chunk(String rawText) {
        List<Chunk> chunks = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return chunks;
        }

        String[] words = rawText.trim().split("\\s+");
        int targetWords = (int) Math.round(TARGET_TOKENS / TOKENS_PER_WORD);   // ~376
        int overlapWords = (int) Math.round(OVERLAP_TOKENS / TOKENS_PER_WORD); // ~38
        int step = Math.max(1, targetWords - overlapWords);

        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + targetWords, words.length);
            String content = String.join(" ", List.of(words).subList(start, end));
            int tokenEstimate = (int) Math.round((end - start) * TOKENS_PER_WORD);
            chunks.add(new Chunk(content, tokenEstimate));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
