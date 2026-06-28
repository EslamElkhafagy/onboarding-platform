package com.onboarding.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_checklist_items")
public class OnboardingChecklistItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "checklist_id", nullable = false)
    private UUID checklistId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public OnboardingChecklistItem() {}

    public UUID getId() { return id; }

    public UUID getChecklistId() { return checklistId; }
    public void setChecklistId(UUID checklistId) { this.checklistId = checklistId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
