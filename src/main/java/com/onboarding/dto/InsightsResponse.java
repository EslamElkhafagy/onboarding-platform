package com.onboarding.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Admin "question insights": volume, gaps, and the most-asked questions. */
public record InsightsResponse(
        long totalQuestions,
        long unansweredCount,
        List<QuestionCount> topQuestions,
        List<Gap> recentGaps
) {
    public record QuestionCount(String question, long count) {}
    public record Gap(String question, OffsetDateTime askedAt) {}
}
