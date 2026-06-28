package com.onboarding.repository;

import com.onboarding.entity.ChecklistTemplateItemDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChecklistTemplateItemDocumentRepository
        extends JpaRepository<ChecklistTemplateItemDocument, UUID> {

    List<ChecklistTemplateItemDocument> findByTemplateItemId(UUID templateItemId);

    List<ChecklistTemplateItemDocument> findByTemplateItemIdIn(Collection<UUID> templateItemIds);
}
