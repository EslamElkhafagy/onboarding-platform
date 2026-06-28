package com.onboarding.repository;

import com.onboarding.entity.ChecklistTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChecklistTemplateItemRepository extends JpaRepository<ChecklistTemplateItem, UUID> {

    List<ChecklistTemplateItem> findByTemplateIdOrderByPosition(UUID templateId);

    long countByTemplateId(UUID templateId);
}
