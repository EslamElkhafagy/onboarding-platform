package com.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Add an item to a template. position is optional — appended to the end when null.
 * documentIds optionally attaches study material (company documents) to the task.
 */
public record TemplateItemRequest(
        @NotBlank String title,
        String description,
        Integer dueDay,
        Integer position,
        List<UUID> documentIds
) {}
