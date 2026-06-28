package com.onboarding.service;

import com.onboarding.ai.EmbeddingClient;
import com.onboarding.entity.Document;
import com.onboarding.entity.DocumentChunk;
import com.onboarding.entity.DocumentStatus;
import com.onboarding.repository.ChunkVectorDao;
import com.onboarding.repository.DocumentChunkRepository;
import com.onboarding.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Card 6 ingestion: extract text from a stored document, split it into overlapping chunks,
 * and persist them. Embeddings (Card 7) run on these chunks afterwards and flip the status
 * to READY; until then a successfully chunked document sits at PROCESSING.
 *
 * Runs synchronously off the upload request for now — moving this to a background job is a
 * Sprint 6 hardening item. Each status change is its own transaction so a FAILED status is
 * still recorded if extraction blows up, and re-ingesting a document is idempotent.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkVectorDao vectorDao;
    private final FileStorageService storage;
    private final TextExtractionService extractor;
    private final ChunkingService chunker;
    private final EmbeddingClient embeddingClient;

    public IngestionService(DocumentRepository documentRepository,
                            DocumentChunkRepository chunkRepository,
                            ChunkVectorDao vectorDao,
                            FileStorageService storage,
                            TextExtractionService extractor,
                            ChunkingService chunker,
                            EmbeddingClient embeddingClient) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorDao = vectorDao;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
    }

    public void ingest(UUID documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Ingestion skipped: document {} not found", documentId);
            return;
        }
        try {
            doc.setStatus(DocumentStatus.PROCESSING);
            doc.setErrorMessage(null);
            documentRepository.save(doc);

            Path path = storage.load(doc.getStorageKey());
            String text = extractor.extract(path, doc.getMimeType(), doc.getFilename());

            chunkRepository.deleteByDocumentId(documentId);
            List<ChunkingService.Chunk> chunks = chunker.chunk(text);

            // Persist chunk text first, then embed and back-fill the vector column (Card 7).
            List<DocumentChunk> saved = new ArrayList<>(chunks.size());
            int index = 0;
            for (ChunkingService.Chunk c : chunks) {
                DocumentChunk entity = new DocumentChunk();
                entity.setDocumentId(documentId);
                entity.setCompanyId(doc.getCompanyId());
                entity.setChunkIndex(index++);
                entity.setContent(c.content());
                entity.setTokenCount(c.tokenCount());
                saved.add(chunkRepository.save(entity));
            }

            if (!saved.isEmpty()) {
                List<String> texts = saved.stream().map(DocumentChunk::getContent).toList();
                List<float[]> embeddings = embeddingClient.embed(texts);
                for (int i = 0; i < saved.size(); i++) {
                    vectorDao.updateEmbedding(saved.get(i).getId(), embeddings.get(i));
                }
            }

            log.info("Ingested document {} into {} embedded chunks", documentId, saved.size());
            doc.setStatus(DocumentStatus.READY);
            documentRepository.save(doc);
        } catch (Exception e) {
            log.error("Ingestion failed for document {}", documentId, e);
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
        }
    }
}
