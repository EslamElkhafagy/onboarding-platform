package com.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MessageView(
        UUID id,
        String role,
        String content,
        Boolean wasAnswered,
        OffsetDateTime createdAt,
        List<SourceView> sources
) {}
