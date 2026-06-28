package com.onboarding.service;

import com.onboarding.entity.AuditLog;
import com.onboarding.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Records notable admin actions to the audit trail. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID companyId, UUID actorUserId, String action, String targetType, UUID targetId) {
        AuditLog entry = new AuditLog();
        entry.setCompanyId(companyId);
        entry.setActorUserId(actorUserId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        auditLogRepository.save(entry);
    }
}
