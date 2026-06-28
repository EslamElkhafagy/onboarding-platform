package com.onboarding.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * pgvector operations that Hibernate can't express: writing the embedding column and
 * cosine-similarity search. Vectors are passed as pgvector's text literal "[v1,v2,...]"
 * and cast server-side. Search is always tenant-scoped by company_id.
 */
@Repository
public class ChunkVectorDao {

    private final JdbcTemplate jdbc;

    public ChunkVectorDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A retrieved chunk with its cosine similarity (1.0 = identical, higher is better). */
    public record Match(UUID chunkId, UUID documentId, String content, double score) {}

    public void updateEmbedding(UUID chunkId, float[] embedding) {
        jdbc.update("UPDATE document_chunks SET embedding = CAST(? AS vector) WHERE id = ?",
                toVectorLiteral(embedding), chunkId);
    }

    /** Searches all of the company's chunks. */
    public List<Match> search(UUID companyId, float[] queryEmbedding, int k) {
        return search(companyId, null, queryEmbedding, k);
    }

    /**
     * Returns the top-k most similar chunks within a company, ordered best-first. When
     * {@code documentId} is non-null, retrieval is restricted to that single document.
     * Uses the cosine distance operator (<=>) backed by the hnsw index; score = 1 - distance.
     */
    public List<Match> search(UUID companyId, UUID documentId, float[] queryEmbedding, int k) {
        String literal = toVectorLiteral(queryEmbedding);
        StringBuilder sql = new StringBuilder("""
                SELECT id, document_id, content,
                       1 - (embedding <=> CAST(? AS vector)) AS score
                FROM document_chunks
                WHERE company_id = ? AND embedding IS NOT NULL
                """);
        List<Object> args = new java.util.ArrayList<>();
        args.add(literal);
        args.add(companyId);
        if (documentId != null) {
            sql.append("  AND document_id = ?\n");
            args.add(documentId);
        }
        sql.append("ORDER BY embedding <=> CAST(? AS vector)\nLIMIT ?");
        args.add(literal);
        args.add(k);

        return jdbc.query(
                sql.toString(),
                (rs, rowNum) -> new Match(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("content"),
                        rs.getDouble("score")),
                args.toArray());
    }

    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
