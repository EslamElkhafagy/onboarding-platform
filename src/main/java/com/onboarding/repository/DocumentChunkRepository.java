package com.onboarding.repository;

import com.onboarding.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    long countByDocumentId(UUID documentId);

    // Cleared before re-ingesting so re-processing a document is idempotent.
    void deleteByDocumentId(UUID documentId);
}
