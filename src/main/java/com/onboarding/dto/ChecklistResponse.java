package com.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChecklistResponse(
        UUID id,
        UUID userId,
        UUID templateId,
        OffsetDateTime assignedAt,
        int totalItems,
        int completedItems,
        List<ChecklistItemResponse> items
) {}
