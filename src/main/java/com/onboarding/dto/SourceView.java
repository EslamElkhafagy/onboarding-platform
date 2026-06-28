package com.onboarding.dto;

import java.util.UUID;

/** A citation: the document/chunk that grounded an answer, with its similarity score. */
public record SourceView(UUID documentId, String filename, UUID chunkId, Float score) {}
