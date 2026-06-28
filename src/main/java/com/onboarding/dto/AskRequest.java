package com.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Ask a question. conversationId is null to start a new conversation. documentId optionally
 * scopes retrieval to a single document (e.g. "ask about this study material"); null searches
 * all of the company's documents.
 */
public record AskRequest(
        UUID conversationId,
        @NotBlank String question,
        UUID documentId
) {}
