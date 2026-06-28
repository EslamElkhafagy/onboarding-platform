package com.onboarding.entity;

/**
 * Lifecycle of an uploaded document. Mirrors the CHECK constraint on documents.status.
 * UPLOADED -> PROCESSING -> READY, or -> FAILED on error.
 */
public enum DocumentStatus {
    UPLOADED,
    PROCESSING,
    READY,
    FAILED
}
