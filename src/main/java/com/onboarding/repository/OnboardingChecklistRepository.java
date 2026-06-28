package com.onboarding.repository;

import com.onboarding.entity.OnboardingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingChecklistRepository extends JpaRepository<OnboardingChecklist, UUID> {

    List<OnboardingChecklist> findByCompanyIdAndUserIdOrderByAssignedAtDesc(UUID companyId, UUID userId);

    List<OnboardingChecklist> findByCompanyId(UUID companyId);

    Optional<OnboardingChecklist> findByIdAndCompanyId(UUID id, UUID companyId);
}
