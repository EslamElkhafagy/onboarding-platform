package com.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        List<MessageView> messages
) {}
