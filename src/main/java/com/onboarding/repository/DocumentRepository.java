package com.onboarding.repository;

import com.onboarding.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Tenant-scoped lookups — always filter by company.
    List<Document> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<Document> findByIdAndCompanyId(UUID id, UUID companyId);
}
