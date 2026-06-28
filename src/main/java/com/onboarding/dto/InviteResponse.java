package com.onboarding.dto;

import java.time.OffsetDateTime;

/**
 * Returned to the admin after inviting a hire. {@code token} is the raw invite token, shown
 * only once here so the admin can share the accept link (no email delivery yet). The hire
 * exchanges it for a password via {@code POST /api/auth/set-password}.
 */
public record InviteResponse(UserResponse user, String token, OffsetDateTime expiresAt) {}
