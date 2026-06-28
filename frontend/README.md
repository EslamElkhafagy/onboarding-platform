# Onboarding Platform — Frontend

Angular 21 SPA (standalone components + signals) for the AI onboarding platform. It talks to
the Spring Boot backend over the REST API documented in `../ARCHITECTURE.md`.

## Run it

```bash
npm install
npm start          # ng serve on http://localhost:4200
```

The dev server proxies `/api` and `/health` to the backend on `http://localhost:8080`
(see `proxy.conf.json`), so start the backend first (`mvn spring-boot:run` in the repo root).

```bash
npm run build      # production build into dist/
npm test           # unit tests (Vitest via ng test)
```

## Structure

```
src/app/
  app.ts / app.html        shell: top nav (role-aware) + <router-outlet>
  app.routes.ts            lazy routes, guarded by authGuard / adminGuard
  models.ts                TypeScript shapes mirroring the backend DTOs
  core/
    auth.ts                AuthService — JWT/user session (localStorage), signals
    auth-interceptor.ts    attaches Bearer token; logs out + redirects on 401
    guards.ts              authGuard (any user) / adminGuard (ADMIN only)
    api.ts                 typed HttpClient wrapper for every endpoint
  features/
    login/                 sign in + register (creates company + first admin)
    chat/                  RAG chat: conversations, ask, citations, "not found" notice
    checklist/             new-hire "my checklist" with completion toggles + progress
    documents/             admin: upload (PDF/DOCX), status, delete
    templates/             admin: create templates + add items
    dashboard/             admin: per-hire progress + question insights
```

## Auth model

Register creates a company and its first ADMIN. ADMINs see Documents / Templates / Dashboard
in the nav; NEW_HIREs see only Chat and My checklist. The token is stored in `localStorage`
and sent on every request by the interceptor; a 401 clears the session and bounces to login.

## Not yet built

- Admin "assign a checklist template to a hire" screen (API: `POST /api/checklists/assign`).
- Invite-user UI (API: `POST /api/auth/invite`).
- Richer loading/empty/error states and toasts.
