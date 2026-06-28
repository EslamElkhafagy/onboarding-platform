package com.onboarding.dto;

import com.onboarding.entity.Document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String mimeType,
        String status,
        String errorMessage,
        UUID uploadedBy,
        OffsetDateTime createdAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFilename(),
                doc.getMimeType(),
                doc.getStatus().name(),
                doc.getErrorMessage(),
                doc.getUploadedBy(),
                doc.getCreatedAt());
    }
}
