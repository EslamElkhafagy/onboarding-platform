package com.onboarding.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false)
    private String content;

    // Set on ASSISTANT messages; false when the grounded-answer fallback fired.
    @Column(name = "was_answered")
    private Boolean wasAnswered;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Message() {}

    public UUID getId() { return id; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getWasAnswered() { return wasAnswered; }
    public void setWasAnswered(Boolean wasAnswered) { this.wasAnswered = wasAnswered; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
