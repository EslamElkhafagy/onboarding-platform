package com.onboarding.repository;

import com.onboarding.entity.OnboardingChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OnboardingChecklistItemRepository extends JpaRepository<OnboardingChecklistItem, UUID> {

    List<OnboardingChecklistItem> findByChecklistIdOrderByPosition(UUID checklistId);

    long countByChecklistId(UUID checklistId);

    long countByChecklistIdAndCompletedTrue(UUID checklistId);
}
