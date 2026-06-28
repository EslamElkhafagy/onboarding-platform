package com.onboarding.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate queries for the admin "question insights" view. Messages carry no company_id, so
 * every query joins through conversations to stay tenant-scoped.
 */
@Repository
public class InsightsDao {

    private final JdbcTemplate jdbc;

    public InsightsDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record QuestionCount(String question, long count) {}
    public record Gap(String question, OffsetDateTime askedAt) {}

    /** {totalQuestions, unanswered} for the company. */
    public Map<String, Long> counts(UUID companyId) {
        return jdbc.queryForObject(
                """
                SELECT
                    COUNT(*) FILTER (WHERE m.role = 'USER') AS total_questions,
                    COUNT(*) FILTER (WHERE m.role = 'ASSISTANT' AND m.was_answered = false) AS unanswered
                FROM messages m
                JOIN conversations c ON c.id = m.conversation_id
                WHERE c.company_id = ?
                """,
                (rs, n) -> Map.of(
                        "totalQuestions", rs.getLong("total_questions"),
                        "unanswered", rs.getLong("unanswered")),
                companyId);
    }

    /** Most frequently asked questions (exact-text grouping). */
    public List<QuestionCount> topQuestions(UUID companyId, int limit) {
        return jdbc.query(
                """
                SELECT m.content AS question, COUNT(*) AS cnt
                FROM messages m
                JOIN conversations c ON c.id = m.conversation_id
                WHERE c.company_id = ? AND m.role = 'USER'
                GROUP BY m.content
                ORDER BY cnt DESC
                LIMIT ?
                """,
                (rs, n) -> new QuestionCount(rs.getString("question"), rs.getLong("cnt")),
                companyId, limit);
    }

    /**
     * Recent questions the assistant couldn't answer — the documentation gaps. Pairs each
     * unanswered assistant reply with the question that prompted it.
     */
    public List<Gap> recentGaps(UUID companyId, int limit) {
        return jdbc.query(
                """
                SELECT u.content AS question, a.created_at AS asked_at
                FROM messages a
                JOIN conversations c ON c.id = a.conversation_id
                JOIN LATERAL (
                    SELECT content
                    FROM messages u
                    WHERE u.conversation_id = a.conversation_id
                      AND u.role = 'USER'
                      AND u.created_at <= a.created_at
                    ORDER BY u.created_at DESC
                    LIMIT 1
                ) u ON true
                WHERE c.company_id = ? AND a.role = 'ASSISTANT' AND a.was_answered = false
                ORDER BY a.created_at DESC
                LIMIT ?
                """,
                (rs, n) -> new Gap(rs.getString("question"),
                        rs.getObject("asked_at", OffsetDateTime.class)),
                companyId, limit);
    }
}
