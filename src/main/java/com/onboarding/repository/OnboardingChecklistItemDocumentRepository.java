package com.onboarding.repository;

import com.onboarding.entity.OnboardingChecklistItemDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OnboardingChecklistItemDocumentRepository
        extends JpaRepository<OnboardingChecklistItemDocument, UUID> {

    List<OnboardingChecklistItemDocument> findByChecklistItemId(UUID checklistItemId);

    List<OnboardingChecklistItemDocument> findByChecklistItemIdIn(Collection<UUID> checklistItemIds);
}
