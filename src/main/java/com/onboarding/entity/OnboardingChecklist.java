package com.onboarding.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A per-hire checklist instance. Items are COPIED from the template at assignment time. */
@Entity
@Table(name = "onboarding_checklists")
public class OnboardingChecklist {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    public OnboardingChecklist() {}

    public UUID getId() { return id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }

    public OffsetDateTime getAssignedAt() { return assignedAt; }
}
