package com.onboarding.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single-use invite token tied to a freshly-created NEW_HIRE user. Only the SHA-256 hash
 * of the token is stored; the raw token is shown to the admin once and shared as a link.
 */
@Entity
@Table(name = "user_invites")
public class UserInvite {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Carried for tenant-scoping consistency with the rest of the schema.
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // Null until the hire accepts the invite by setting a password; non-null marks it spent.
    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UserInvite() {}

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
