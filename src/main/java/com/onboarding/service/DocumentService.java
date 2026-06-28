package com.onboarding.service;

import com.onboarding.config.ApiException;
import com.onboarding.dto.DocumentResponse;
import com.onboarding.entity.Document;
import com.onboarding.entity.DocumentStatus;
import com.onboarding.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String PDF_MIME = "application/pdf";
    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final Set<String> ALLOWED_MIME = Set.of(PDF_MIME, DOCX_MIME);
    private static final Set<String> ALLOWED_EXT = Set.of(".pdf", ".docx");

    private final DocumentRepository documentRepository;
    private final FileStorageService storage;

    public DocumentService(DocumentRepository documentRepository, FileStorageService storage) {
        this.documentRepository = documentRepository;
        this.storage = storage;
    }

    /**
     * Stores an uploaded PDF/DOCX for the caller's company and records it with status UPLOADED.
     * Text extraction + embeddings (Cards 6/7) run later off this row.
     */
    @Transactional
    public DocumentResponse upload(UUID companyId, UUID uploadedBy, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "No file was uploaded");
        }
        String filename = file.getOriginalFilename();
        validateType(filename, file.getContentType());

        // Persist the row first so the generated id can name the stored file.
        Document doc = new Document();
        doc.setCompanyId(companyId);
        doc.setUploadedBy(uploadedBy);
        doc.setFilename(filename);
        doc.setMimeType(file.getContentType());
        doc.setStorageKey("pending");
        doc.setStatus(DocumentStatus.UPLOADED);
        doc = documentRepository.save(doc);

        String storageKey = storage.store(companyId, doc.getId(), filename, file);
        doc.setStorageKey(storageKey);
        doc = documentRepository.save(doc);

        return DocumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID id, UUID companyId) {
        Document doc = documentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Document not found"));
        return DocumentResponse.from(doc);
    }

    /** The stored file plus the metadata a controller needs to serve it for viewing/download. */
    public record DownloadView(Path path, String filename, String mimeType) {}

    /**
     * Resolves a company-scoped document to its on-disk file for streaming. Any authenticated
     * user in the company may read documents (so new hires can study them).
     */
    @Transactional(readOnly = true)
    public DownloadView load(UUID companyId, UUID id) {
        Document doc = documentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Document not found"));
        Path path = storage.load(doc.getStorageKey());
        if (!Files.isReadable(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "The stored file is unavailable");
        }
        String mime = doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream";
        return new DownloadView(path, doc.getFilename(), mime);
    }

    /**
     * Deletes a document and its stored file. Chunks and message_sources are removed by the
     * ON DELETE CASCADE foreign keys in the schema.
     */
    @Transactional
    public void delete(UUID companyId, UUID id) {
        Document doc = documentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Document not found"));
        storage.delete(doc.getStorageKey());
        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID companyId) {
        return documentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    private void validateType(String filename, String contentType) {
        boolean okMime = contentType != null && ALLOWED_MIME.contains(contentType);
        boolean okExt = filename != null && ALLOWED_EXT.stream().anyMatch(
                ext -> filename.toLowerCase().endsWith(ext));
        // Browsers/clients are inconsistent with multipart content types, so accept a
        // match on either the declared MIME type or the filename extension.
        if (!okMime && !okExt) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_TYPE",
                    "Only PDF and DOCX files are supported");
        }
    }
}
