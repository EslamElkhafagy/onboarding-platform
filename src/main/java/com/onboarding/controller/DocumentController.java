package com.onboarding.controller;

import com.onboarding.dto.DocumentResponse;
import com.onboarding.security.AuthPrincipal;
import com.onboarding.service.AuditService;
import com.onboarding.service.DocumentService;
import com.onboarding.service.IngestionService;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final IngestionService ingestionService;
    private final AuditService auditService;

    public DocumentController(DocumentService documentService,
                             IngestionService ingestionService,
                             AuditService auditService) {
        this.documentService = documentService;
        this.ingestionService = ingestionService;
        this.auditService = auditService;
    }

    /** Admin uploads a PDF/DOCX into their own company's document library. */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal AuthPrincipal me,
                                                   @RequestParam("file") MultipartFile file) {
        DocumentResponse doc = documentService.upload(me.companyId(), me.userId(), file);
        // Upload has committed, so the row is visible; extract + chunk + embed it (Cards 6/7).
        ingestionService.ingest(doc.id());
        auditService.record(me.companyId(), me.userId(), "DOCUMENT_UPLOADED", "document", doc.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.get(doc.id(), me.companyId()));
    }

    /** Lists the caller company's documents, newest first. */
    @GetMapping
    public List<DocumentResponse> list(@AuthenticationPrincipal AuthPrincipal me) {
        return documentService.list(me.companyId());
    }

    /**
     * Streams a document's file for in-browser viewing/download. Available to any authenticated
     * user in the company so new hires can study the material attached to their tasks.
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@AuthenticationPrincipal AuthPrincipal me,
                                            @PathVariable UUID id) {
        DocumentService.DownloadView dv = documentService.load(me.companyId(), id);
        Resource resource = new PathResource(dv.path());
        String safeName = URLEncoder.encode(dv.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dv.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + safeName)
                .body(resource);
    }

    /** Admin deletes a document, its file, chunks, and citations (data deletion). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable UUID id) {
        documentService.delete(me.companyId(), id);
        auditService.record(me.companyId(), me.userId(), "DOCUMENT_DELETED", "document", id);
        return ResponseEntity.noContent().build();
    }
}
