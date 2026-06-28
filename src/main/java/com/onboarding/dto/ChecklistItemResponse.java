package com.onboarding.dto;

import com.onboarding.entity.OnboardingChecklistItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChecklistItemResponse(
        UUID id,
        String title,
        String description,
        Integer dueDay,
        int position,
        boolean completed,
        OffsetDateTime completedAt,
        List<DocumentRefView> documents
) {
    public static ChecklistItemResponse from(OnboardingChecklistItem item, List<DocumentRefView> documents) {
        return new ChecklistItemResponse(item.getId(), item.getTitle(), item.getDescription(),
                item.getDueDay(), item.getPosition(), item.isCompleted(), item.getCompletedAt(), documents);
    }
}
