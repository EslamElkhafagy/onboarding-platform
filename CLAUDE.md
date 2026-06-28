# CLAUDE.md — Project Context for Claude Code

> This file is read automatically by Claude Code on launch. It carries the full context
> of the project so any new session continues where the planning left off.

## What this project is
An **AI-powered employee onboarding platform**. A new hire chats with an assistant that
answers questions from their company's documents (RAG over uploaded files) and works
through an onboarding checklist. Admins (HR) upload docs, build checklists, and see
progress + the most-asked questions to find documentation gaps.

Built solo, nights/weekends. The owner's background is Java/Spring, so the stack plays to
that strength.

## Stack
- **Backend:** Spring Boot 3.3, Java 17
- **DB:** PostgreSQL + pgvector (embeddings stored in `document_chunks.embedding`)
- **Migrations:** Flyway (schema is Flyway-owned; Hibernate is `ddl-auto: validate`)
- **Auth:** JWT (jjwt), Spring Security, BCrypt, role-based (ADMIN / NEW_HIRE)
- **Frontend:** Angular 21 (standalone components + signals) in `frontend/`; talks to the API
  via `/api` (dev proxy → :8080). Screens: login/register, chat, my-checklist, and admin
  documents/templates/dashboard. Build: `cd frontend && npm install && npm run build`.
- **Local DB:** `docker compose up -d` (pgvector/pgvector:pg16)

## Core rules (keep these invariant)
- **Tenant isolation:** every customer-owned row has `company_id`. Every query is filtered
  by the authenticated user's `company_id` (from their JWT, surfaced as `AuthPrincipal`).
  Repositories expose `...AndCompanyId` finders for this. Never trust a client-supplied
  company id.
- **Checklists:** templates vs per-hire instances are separate. When a template is assigned,
  its items are COPIED into the hire's checklist, so later edits to a template don't rewrite
  in-progress checklists.
- **RAG answers:** must be grounded in retrieved chunks; if nothing relevant is found, say so
  and point to a human, and set `was_answered = false` (used for the insights feature).
- **Citations:** answers track which chunks/documents they used (`message_sources`).

## Current status
**Sprints 1–5 + light Sprint 6 are built and compile (Java 17, `mvn test` green).** Not yet
run against a live DB — no Docker/Postgres in the dev box, so the full request paths haven't
been exercised end-to-end. AI calls need keys (see below).

- **Sprint 1:** skeleton (`/health`), schema V1–V4, JWT auth, tenant scoping, role checks.
- **Sprint 2:** document upload (`/api/documents`), ingestion = PDFBox/POI extraction +
  ~500-token chunking + OpenAI embeddings → pgvector.
- **Sprint 3:** RAG chat (`/api/chat/ask`) grounded in retrieved chunks, citations
  (`message_sources`), fallback sets `was_answered=false`. Conversations API.
- **Sprint 4:** checklist templates + assignment (items copied) + per-hire instances + completion.
- **Sprint 5:** admin dashboard `/api/admin/progress` and `/api/admin/insights`.
- **Sprint 6 (light):** audit log + document data-deletion + retry/backoff on AI calls
  (`ai/AiRetry.java`, used by both AI clients; knobs under `app.ai.retry.*`). Remaining: real pilot.

> Build note: Java was lowered 21→17 (only JDK 17 installed locally; Spring Boot 3.3 supports it).
> AI providers: embeddings = OpenAI `text-embedding-3-small` (1536, matches schema); chat =
> Anthropic Claude. Set `OPENAI_API_KEY` and `ANTHROPIC_API_KEY`; knobs under `app.ai.*`/`app.rag.*`.
> See `ARCHITECTURE.md` for full schema + API contracts.
>
> **Provider flag (`AI_PROVIDER` / `app.ai.provider`):** swaps which `ChatClient`+`EmbeddingClient`
> beans are wired, via `@ConditionalOnProperty` (no code change to switch). Values:
> - `paid` (default) — OpenAI embeddings + Anthropic chat. Needs both keys, costs money.
> - `free` — built-in offline stub (`LocalStubEmbeddingClient`/`LocalStubChatClient`): hashed
>   keyword embeddings + extractive answers. No keys, no network, $0; rough quality. Good for
>   running the full ingest→retrieve→answer pipeline locally.
> - `ollama` — local models via Ollama (`OllamaEmbeddingClient`/`OllamaChatClient`). Free to run;
>   needs Ollama up + models pulled (`llama3.1`, `nomic-embed-text`). nomic-embed is 768-dim and is
>   zero-padded to the schema's vector(1536) — no migration needed.
>
> Embeddings differ per provider, so **re-ingest documents after switching `AI_PROVIDER`** (old
> vectors aren't comparable to a new provider's query vectors).

## Build & run
```bash
docker compose up -d          # start Postgres + pgvector
mvn spring-boot:run           # Flyway runs migrations on boot
curl http://localhost:8080/health   # {"status":"UP"}
```
Auth flow examples are in README.md.

## Project layout
```
src/main/java/com/onboarding/
  OnboardingPlatformApplication.java
  config/        SecurityConfig, ApiException, GlobalExceptionHandler
  controller/    HealthController, AuthController
  dto/           request/response records
  entity/        Company, User, Role
  repository/    CompanyRepository, UserRepository
  security/      JwtService, JwtAuthFilter, AuthPrincipal
  service/       AuthService
src/main/resources/
  application.yml
  db/migration/  V1__companies_and_users, V2__documents_and_chunks,
                 V3__checklists, V4__conversations_and_messages
```

## Roadmap (2-week sprints, ~16 pts each, solo)
- **Sprint 1 — DONE:** foundation skeleton, schema, auth, invites.
- **Sprint 2 — DONE:** document upload + ingestion (PDFBox/POI extraction, ~500-token
  chunking w/ overlap, OpenAI embeddings → pgvector).
- **Sprint 3 — DONE:** RAG answer flow + citations (chat UI pending frontend).
- **Sprint 4 — DONE:** checklist templates + per-hire instances + assignment.
- **Sprint 5 — DONE:** admin dashboard (progress) + question insights.
- **Sprint 6 — IN PROGRESS:** pilot hardening — data deletion, audit log, and AI retries
  are DONE. Remaining: end-to-end run against a live Postgres, then a real pilot.

The full backlog is 19 user-story cards across these 6 sprints (kept in Trello: lists are
workflow stages Backlog→To Do→Doing→Review→Done; Epic + Sprint as labels; points in card
titles; sub-tasks as checklists).

## How to work with me here
- Read `ARCHITECTURE.md` for the full schema + API contracts before adding endpoints.
- Follow the existing patterns (DTO records, service layer, `...AndCompanyId` repo methods,
  `ApiException` for errors).
- Make migrations additive (new V5, V6... files — never edit applied migrations).
- Build and run after changes; fix compile/test errors before moving on.

## Immediate next task
The backend for Sprints 1–5 is built and `mvn test` is green (8 tests). Sprint 6 hardening
(audit log, data deletion, AI retries) is done. The two remaining tracks are:
1. **Live-DB shakedown:** `docker compose up -d`, `mvn spring-boot:run`, then exercise the
   real request paths (auth → upload → ingest → ask → checklist → admin) end-to-end with keys
   set. Nothing has run against Postgres yet, so this is where the first real bugs will surface.
2. **Frontend:** Angular 21 app scaffolded in `frontend/` with the core screens (auth, chat,
   checklist, admin docs/templates/dashboard) wired to the API and building clean. Next:
   exercise it against the running backend, then polish (loading/empty/error states, an
   admin "assign checklist to a hire" flow, invite-user UI).
