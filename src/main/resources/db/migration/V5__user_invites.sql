-- V5__user_invites.sql
-- Single-use, expiring invite tokens that let an admin-invited hire set their own
-- password and activate their account. We store only a hash of the token, never the
-- raw value, so a leaked DB cannot be used to accept invites.

CREATE TABLE user_invites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_invites_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_user_invites_user_id ON user_invites(user_id);
