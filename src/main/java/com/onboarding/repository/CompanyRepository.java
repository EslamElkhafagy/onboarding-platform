package com.onboarding.repository;

import com.onboarding.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}
