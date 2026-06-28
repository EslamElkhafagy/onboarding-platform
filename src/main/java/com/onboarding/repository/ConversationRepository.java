package com.onboarding.repository;

import com.onboarding.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Tenant + ownership scoped: a user only ever sees their own conversations.
    Optional<Conversation> findByIdAndUserIdAndCompanyId(UUID id, UUID userId, UUID companyId);
}
