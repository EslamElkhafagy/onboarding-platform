package com.onboarding.dto;

import java.util.List;
import java.util.UUID;

/** The assistant's reply to a question, with citations. */
public record AnswerResponse(
        UUID conversationId,
        UUID messageId,
        String answer,
        boolean wasAnswered,
        List<SourceView> sources
) {}
