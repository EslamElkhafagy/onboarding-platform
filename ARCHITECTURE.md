# ARCHITECTURE.md — Schema + API Contracts

Reference for the onboarding platform. Read this before adding endpoints. Patterns to follow:
DTO records, a service layer, `...AndCompanyId` repository finders for tenant scoping, and
`ApiException(status, code, message)` for errors (rendered by `GlobalExceptionHandler` as
`{"error": {"code", "message"}}`).

## Tenant isolation (invariant)
Every customer-owned row has `company_id`. Every query is filtered by the authenticated
user's `company_id`, taken from their JWT and surfaced as `AuthPrincipal` (never from client
input). Rows without a direct `company_id` (template/checklist items, messages, sources) are
reached only through a parent that is company-scoped.

## Data model (Flyway V1–V6, schema-owned; Hibernate `ddl-auto: validate`)

### V1 — foundation
- `companies(id, name, created_at)`
- `users(id, company_id→companies, email UNIQUE, password_hash, role CHECK ADMIN|NEW_HIRE, full_name, start_date, created_at)`

### V2 — documents + chunks
- `documents(id, company_id, uploaded_by→users, filename, storage_key, mime_type, status CHECK UPLOADED|PROCESSING|READY|FAILED, error_message, created_at)`
- `document_chunks(id, document_id→documents, company_id, chunk_index, content, token_count, embedding vector(1536), created_at)` — hnsw cosine index on `embedding`

### V3 — checklists (templates vs per-hire instances)
- `checklist_templates(id, company_id, name, created_by, created_at)`
- `checklist_template_items(id, template_id, title, description, due_day, position)`
- `onboarding_checklists(id, company_id, user_id, template_id, assigned_by, assigned_at)`
- `onboarding_checklist_items(id, checklist_id, title, description, due_day, position, completed, completed_at)`

Items are **copied** from the template at assignment time, so editing a template never
rewrites an in-progress checklist.

### V4 — chat, citations, audit
- `conversations(id, company_id, user_id, title, created_at)`
- `messages(id, conversation_id, role CHECK USER|ASSISTANT, content, was_answered, created_at)`
- `message_sources(id, message_id, document_id, chunk_id, score)` — citation trail
- `audit_logs(id, company_id, actor_user_id, action, target_type, target_id, created_at)`

`message_sources`, `document_chunks` cascade on document delete (data deletion).

### V5 — new-hire invites
- `user_invites(id, user_id→users, company_id, token_hash UNIQUE, expires_at, accepted_at, created_at)`

Single-use, expiring tokens that let an admin-invited hire set their own password. Only the
SHA-256 **hash** of the token is stored (raw token shown to the admin once); `accepted_at`
marks it spent. Cascades on user delete.

### V6 — study documents on checklist tasks
- `checklist_template_item_documents(id, template_item_id→checklist_template_items, document_id→documents, UNIQUE(template_item_id, document_id))`
- `onboarding_checklist_item_documents(id, checklist_item_id→onboarding_checklist_items, document_id→documents, UNIQUE(checklist_item_id, document_id))`

Attaches study material to a task. Like the items themselves, template-item links are **copied**
into instance-item links at assignment, so editing a template never rewrites an in-progress
checklist. Both cascade on item or document delete.

## RAG pipeline
1. **Upload** (`POST /api/documents`) stores the file (filesystem, tenant-namespaced) + a row (`UPLOADED`).
2. **Ingest** (synchronous, off the upload request): extract text (PDFBox/POI) → chunk
   (~500 tokens, ~50 overlap) → embed (OpenAI `text-embedding-3-small`, 1536) → write
   `document_chunks.embedding`. Status `PROCESSING` → `READY`, or `FAILED` with a message.
3. **Ask** (`POST /api/chat/ask`): embed the question → cosine search `document_chunks`
   (company-scoped, top-k, score ≥ `app.rag.min-score`) → if nothing relevant, return a
   fallback and set `was_answered=false` (feeds insights); otherwise Claude answers grounded
   in the retrieved chunks and `message_sources` records the citations. An optional
   `documentId` restricts retrieval to a single document (`ChunkVectorDao.search` adds
   `AND document_id = ?`) — used by the "ask about this study material" flow.

pgvector reads/writes go through `ChunkVectorDao` (JdbcTemplate) — Hibernate doesn't map the
`vector` type. Embeddings/LLM are behind `EmbeddingClient` / `ChatClient` interfaces.

## API contracts
All under `/api`. Auth: `Authorization: Bearer <jwt>`. Public: `/health`,
`/api/auth/register`, `/api/auth/login`, `/api/auth/invitations/{token}`,
`/api/auth/set-password`. Admin-only endpoints use `@PreAuthorize("hasRole('ADMIN')")`.

### Auth
- `POST /api/auth/register` → `{token, user}` (creates company + first ADMIN)
- `POST /api/auth/login` → `{token, user}`
- `POST /api/auth/invite` (admin) `{email, fullName, startDate?}` → `{user, token, expiresAt}`
  — creates a NEW_HIRE (no usable password yet) and issues a single-use invite token; the raw
  token is returned once so the admin can share the accept link (no email delivery yet)
- `GET  /api/auth/invitations/{token}` (public) → `{email, fullName, companyName}` — preview an
  invite to greet the hire; `400 INVALID_INVITE` / `410 INVITE_USED` / `410 INVITE_EXPIRED`
- `POST /api/auth/set-password` (public) `{token, password}` → `{token, user}` — hire sets a
  password (min 8), token is marked spent, and they're logged straight in
- `GET  /api/auth/me` → current user

### Documents (admin for write; any authenticated user reads)
- `POST   /api/documents` (admin, multipart `file`, PDF/DOCX) → 201 document (ingested)
- `GET    /api/documents` → company documents, newest first
- `GET    /api/documents/{id}/content` → streams the file (`Content-Disposition: inline`,
  company-scoped) so hires can read study material; `404 FILE_MISSING` if the file is gone
- `DELETE /api/documents/{id}` (admin) → 204; deletes file + chunks + citations

### Chat (any authenticated user)
- `POST /api/chat/ask` `{conversationId?, question, documentId?}` → `{conversationId, messageId, answer, wasAnswered, sources[]}` — `documentId` scopes retrieval to one document
- `GET  /api/conversations` → caller's conversations
- `GET  /api/conversations/{id}` → conversation with messages + sources

### Checklists
- `POST /api/checklist-templates` (admin) `{name}` → template
- `GET  /api/checklist-templates` (admin) → templates with items
- `GET  /api/checklist-templates/{id}` (admin) → template with items
- `POST /api/checklist-templates/{id}/items` (admin) `{title, description?, dueDay?, position?, documentIds?}` → item; `documentIds` attaches study documents (must belong to the company)
- `POST /api/checklists/assign` (admin) `{templateId, userId}` → instance (items + their document links copied)
- `GET  /api/checklists/me` → caller's checklist instances (with progress)
- `GET  /api/checklists/user/{userId}` (admin) → a hire's instances
- `PATCH /api/checklists/items/{itemId}` `{completed}` → item (owner or admin)

Template and checklist item responses include `documents[]` (`{id, filename}`) for any attached study material.

### Admin dashboard (admin)
- `GET /api/admin/progress` → per-hire `{checklistCount, totalItems, completedItems, percentComplete}`
- `GET /api/admin/insights` → `{totalQuestions, unansweredCount, topQuestions[], recentGaps[]}`

## AI configuration (env)
- `OPENAI_API_KEY` — embeddings (required for ingestion + retrieval)
- `ANTHROPIC_API_KEY` — grounded chat answers
- Model/threshold knobs live under `app.ai.*` and `app.rag.*` in `application.yml`.
The app boots without keys; AI calls fail with a clear `*_UNCONFIGURED` error until set.
