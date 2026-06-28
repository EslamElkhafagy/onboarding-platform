package com.onboarding.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "checklist_template_items")
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(nullable = false)
    private int position;

    public ChecklistTemplateItem() {}

    public UUID getId() { return id; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
