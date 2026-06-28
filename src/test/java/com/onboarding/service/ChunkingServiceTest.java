package com.onboarding.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private final ChunkingService chunker = new ChunkingService();

    @Test
    void blankTextProducesNoChunks() {
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
        assertTrue(chunker.chunk(null).isEmpty());
    }

    @Test
    void shortTextProducesSingleChunk() {
        List<ChunkingService.Chunk> chunks = chunker.chunk("just a few words here");
        assertEquals(1, chunks.size());
        assertEquals("just a few words here", chunks.get(0).content());
        assertTrue(chunks.get(0).tokenCount() > 0);
    }

    @Test
    void longTextSplitsIntoOverlappingChunks() {
        // 1000 distinct words -> several chunks of ~376 words each.
        String text = IntStream.range(0, 1000)
                .mapToObj(i -> "w" + i)
                .collect(Collectors.joining(" "));

        List<ChunkingService.Chunk> chunks = chunker.chunk(text);

        assertTrue(chunks.size() > 1, "expected multiple chunks");

        // Consecutive chunks should overlap: the start of chunk 2 appears inside chunk 1.
        String[] firstWords = chunks.get(0).content().split(" ");
        String secondChunkStart = chunks.get(1).content().split(" ")[0];
        assertTrue(List.of(firstWords).contains(secondChunkStart),
                "expected overlap between consecutive chunks");

        // No chunk should blow far past the target size.
        assertTrue(chunks.stream().allMatch(c -> c.tokenCount() <= 520));
    }
}
