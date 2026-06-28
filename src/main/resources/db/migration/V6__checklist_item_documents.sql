-- V6__checklist_item_documents.sql
-- Links documents to checklist tasks, so an admin can attach study material to an item and
-- the hire sees it on their checklist. Templates and per-hire instances are kept separate
-- (mirroring the items themselves): template-item links are copied into instance-item links
-- at assignment time, so editing a template never rewrites an in-progress checklist.

CREATE TABLE checklist_template_item_documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_item_id  UUID NOT NULL REFERENCES checklist_template_items(id) ON DELETE CASCADE,
    document_id       UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uq_ctid_item_doc UNIQUE (template_item_id, document_id)
);

CREATE INDEX idx_ctid_template_item ON checklist_template_item_documents(template_item_id);

CREATE TABLE onboarding_checklist_item_documents (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checklist_item_id  UUID NOT NULL REFERENCES onboarding_checklist_items(id) ON DELETE CASCADE,
    document_id        UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uq_ocid_item_doc UNIQUE (checklist_item_id, document_id)
);

CREATE INDEX idx_ocid_checklist_item ON onboarding_checklist_item_documents(checklist_item_id);
