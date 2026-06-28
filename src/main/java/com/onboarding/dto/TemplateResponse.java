package com.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        OffsetDateTime createdAt,
        List<TemplateItemResponse> items
) {}
