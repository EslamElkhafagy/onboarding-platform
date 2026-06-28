-- V4__conversations_and_messages.sql
-- Chat history, citations, and the admin audit trail.

CREATE TABLE conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_user ON conversations(user_id);

CREATE TABLE messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role             TEXT NOT NULL CHECK (role IN ('USER','ASSISTANT')),
    content          TEXT NOT NULL,
    was_answered     BOOLEAN,  -- set on ASSISTANT messages; false if fallback fired
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);

CREATE TABLE message_sources (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_id     UUID NOT NULL REFERENCES document_chunks(id) ON DELETE CASCADE,
    score        REAL
);

CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    actor_user_id  UUID REFERENCES users(id),
    action         TEXT NOT NULL,
    target_type    TEXT,
    target_id      UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
