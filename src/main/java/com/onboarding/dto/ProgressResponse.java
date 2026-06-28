package com.onboarding.dto;

import java.util.UUID;

/** A new hire's aggregate onboarding progress across all their assigned checklists. */
public record ProgressResponse(
        UUID userId,
        String fullName,
        String email,
        int checklistCount,
        int totalItems,
        int completedItems,
        int percentComplete
) {}
