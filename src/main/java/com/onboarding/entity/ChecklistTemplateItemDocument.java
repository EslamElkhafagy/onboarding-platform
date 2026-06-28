package com.onboarding.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** Join row linking a template item to a document (study material) attached to that task. */
@Entity
@Table(name = "checklist_template_item_documents")
public class ChecklistTemplateItemDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_item_id", nullable = false)
    private UUID templateItemId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    public ChecklistTemplateItemDocument() {}

    public ChecklistTemplateItemDocument(UUID templateItemId, UUID documentId) {
        this.templateItemId = templateItemId;
        this.documentId = documentId;
    }

    public UUID getId() { return id; }

    public UUID getTemplateItemId() { return templateItemId; }
    public void setTemplateItemId(UUID templateItemId) { this.templateItemId = templateItemId; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
}
