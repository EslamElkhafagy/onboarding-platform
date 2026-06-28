package com.onboarding.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** Join row linking a per-hire checklist item to a document, copied from the template at assignment. */
@Entity
@Table(name = "onboarding_checklist_item_documents")
public class OnboardingChecklistItemDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "checklist_item_id", nullable = false)
    private UUID checklistItemId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    public OnboardingChecklistItemDocument() {}

    public OnboardingChecklistItemDocument(UUID checklistItemId, UUID documentId) {
        this.checklistItemId = checklistItemId;
        this.documentId = documentId;
    }

    public UUID getId() { return id; }

    public UUID getChecklistItemId() { return checklistItemId; }
    public void setChecklistItemId(UUID checklistItemId) { this.checklistItemId = checklistItemId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
}
