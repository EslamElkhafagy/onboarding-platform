package com.onboarding.dto;

import com.onboarding.entity.Conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(UUID id, String title, OffsetDateTime createdAt) {

    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.getId(), c.getTitle(), c.getCreatedAt());
    }
}
