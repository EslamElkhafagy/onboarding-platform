package com.onboarding.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** A retrieved chunk that grounded an assistant answer — the citation trail. */
@Entity
@Table(name = "message_sources")
public class MessageSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column
    private Float score;

    public MessageSource() {}

    public UUID getId() { return id; }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public UUID getChunkId() { return chunkId; }
    public void setChunkId(UUID chunkId) { this.chunkId = chunkId; }

    public Float getScore() { return score; }
    public void setScore(Float score) { this.score = score; }
}
