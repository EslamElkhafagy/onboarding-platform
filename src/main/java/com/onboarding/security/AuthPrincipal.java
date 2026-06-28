package com.onboarding.security;

import com.onboarding.entity.Role;

import java.util.UUID;

/**
 * The authenticated caller. Stored as the Authentication principal so controllers
 * and services can read the user's id, company, and role for tenant scoping.
 */
public record AuthPrincipal(UUID userId, UUID companyId, Role role, String email) {
}
