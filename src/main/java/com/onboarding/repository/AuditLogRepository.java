package com.onboarding.repository;

import com.onboarding.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
