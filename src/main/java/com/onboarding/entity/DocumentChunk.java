package com.onboarding.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A ~500-token slice of a document's text. Embeddings (the vector(1536) column) are
 * populated later in Card 7, so the embedding column is intentionally not mapped here;
 * Hibernate validate allows unmapped columns.
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    // Carried on every chunk so similarity search can filter by tenant without a join.
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false)
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public DocumentChunk() {}

    public UUID getId() { return id; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
