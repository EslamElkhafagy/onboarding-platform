-- V3__checklists.sql
-- Templates (reusable) + per-hire instances (copied at assignment).

CREATE TABLE checklist_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE checklist_template_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id  UUID NOT NULL REFERENCES checklist_templates(id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    description  TEXT,
    due_day      INT,
    position     INT NOT NULL
);

CREATE TABLE onboarding_checklists (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id  UUID REFERENCES checklist_templates(id),
    assigned_by  UUID REFERENCES users(id),
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_onboarding_checklists_user ON onboarding_checklists(user_id);

CREATE TABLE onboarding_checklist_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checklist_id  UUID NOT NULL REFERENCES onboarding_checklists(id) ON DELETE CASCADE,
    title         TEXT NOT NULL,
    description   TEXT,
    due_day       INT,
    position      INT NOT NULL,
    completed     BOOLEAN NOT NULL DEFAULT false,
    completed_at  TIMESTAMPTZ
);
