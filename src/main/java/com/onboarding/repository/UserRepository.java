package com.onboarding.repository;

import com.onboarding.entity.Role;
import com.onboarding.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Tenant-scoped lookups — always filter by company.
    List<User> findByCompanyId(UUID companyId);

    List<User> findByCompanyIdAndRole(UUID companyId, Role role);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);
}
