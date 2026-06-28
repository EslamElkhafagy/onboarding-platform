package com.onboarding.dto;

import com.onboarding.entity.ChecklistTemplateItem;

import java.util.List;
import java.util.UUID;

public record TemplateItemResponse(
        UUID id,
        String title,
        String description,
        Integer dueDay,
        int position,
        List<DocumentRefView> documents
) {
    public static TemplateItemResponse from(ChecklistTemplateItem item, List<DocumentRefView> documents) {
        return new TemplateItemResponse(item.getId(), item.getTitle(), item.getDescription(),
                item.getDueDay(), item.getPosition(), documents);
    }
}
