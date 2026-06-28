package com.onboarding.repository;

import com.onboarding.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID> {

    List<ChecklistTemplate> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<ChecklistTemplate> findByIdAndCompanyId(UUID id, UUID companyId);
}
