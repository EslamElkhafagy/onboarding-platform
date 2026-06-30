# Deployment (free tier)

Deploy the **whole platform** — Angular SPA + Spring Boot API — as **one Docker web service**,
backed by a **free Postgres** with pgvector. Because the app is multi-tenant (`company_id`
isolation), a **single deployment serves all customers**; you onboard each company by creating
its admin and sending invites.

**Cost: $0.** RAG runs on the built-in offline stub (`AI_PROVIDER=free`) — no API keys, no
network calls. Flip to real models later by setting `AI_PROVIDER=paid` + keys.

```
┌────────────────────────────┐        ┌──────────────────────────┐
│  Render (free web service) │        │  Supabase / Neon         │
│  Docker container:         │  JDBC  │  Postgres + pgvector     │
│   • Spring Boot API /api   │ ─────► │  (free tier)             │
│   • Angular SPA  /         │        │                          │
└────────────────────────────┘        └──────────────────────────┘
```

---

## 1. Provision the database (Supabase)

1. Create a project at supabase.com (free). Pick a region near your users; set a DB password.
2. **Database → Extensions →** enable **`vector`**. (Flyway's `CREATE EXTENSION IF NOT EXISTS
   vector` also covers this, but enabling it up front avoids permission surprises.)
3. **Project Settings → Database → Connection string → JDBC.** You'll get host, port, db, user.
   Build the three env values the app needs:
   - `DB_URL` = `jdbc:postgresql://<host>:5432/postgres?sslmode=require`
   - `DB_USER` = `postgres` (or the project user shown)
   - `DB_PASSWORD` = the password you set

> **Neon instead?** Create a project, run `CREATE EXTENSION vector;` in the SQL editor, and use
> its JDBC string (also `sslmode=require`). Everything else is identical.

> **Connection limits:** the `prod` profile caps the pool at 5 (`DB_POOL_MAX`). If you use a
> provider's *pooled* (pgBouncer) endpoint in transaction mode, keep the pool small — it's
> already tuned for that.

---

## 2. Deploy the app (Render)

**Option A — Blueprint (recommended):** `render.yaml` is in the repo root.

1. Push this repo to GitHub.
2. Render → **New → Blueprint** → select the repo. It reads `render.yaml` and creates the web
   service (Docker, free plan, health check `/health`).
3. When prompted, fill the three `sync:false` secrets: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
   `JWT_SECRET` is auto-generated; `AI_PROVIDER=free` and `SPRING_PROFILES_ACTIVE=prod` are preset.
4. **Create** → first build runs the multi-stage Dockerfile (Angular build → jar → JRE). On boot,
   Flyway applies V1–V6. Watch logs for `Started OnboardingPlatformApplication`.

**Option B — manual:** Render → New → **Web Service** → repo → Runtime **Docker** → Plan **Free**
→ add the env vars from `render.yaml` by hand.

> **Koyeb / Fly.io / Oracle Always Free** work the same way — point them at the Dockerfile and set
> the same env vars. Oracle's Always Free ARM VM gives the most RAM (no cold starts) if the 512 MB
> hosts feel tight; the trade-off is you manage the VM yourself.

---

## 3. Environment variables

| Var | Required | Value / notes |
|---|---|---|
| `DB_URL` | ✅ | `jdbc:postgresql://<host>:5432/<db>?sslmode=require` |
| `DB_USER` | ✅ | DB user |
| `DB_PASSWORD` | ✅ | DB password |
| `JWT_SECRET` | ✅ | ≥32 random chars (Render auto-generates) |
| `AI_PROVIDER` | ✅ | `free` for the $0 pilot; `paid` for real RAG |
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` (Hikari pool tuning) |
| `PORT` | — | Injected by the host; Spring reads it automatically |
| `STORAGE_DIR` | — | Defaults to `/app/data/uploads` (set in the image) |
| `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` | only if `AI_PROVIDER=paid` | provider keys |

---

## 4. Smoke test (after first deploy)

Replace `$APP` with your Render URL (e.g. `https://onboarding-platform.onrender.com`).
The free instance sleeps when idle — the **first** request after a redeploy may take ~30–60 s
(JVM cold start).

```bash
# 1. Health
curl $APP/health                      # {"status":"UP"}

# 2. Register the first company + admin
curl -X POST $APP/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"companyName":"Acme","email":"admin@acme.com","password":"Passw0rd!","fullName":"Admin"}'
#   -> returns a JWT. Save it:  TOKEN=<token>

# 3. Upload a doc (ingest -> chunk -> embed runs on the free stub)
curl -X POST $APP/api/documents -H "Authorization: Bearer $TOKEN" -F file=@handbook.pdf

# 4. Ask a grounded question
curl -X POST $APP/api/chat/ask -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"question":"What is the PTO policy?"}'
```

Then open `$APP/` in a browser — the Angular app loads from the same origin (no CORS), log in
with the admin you just created, and walk the screens.

---

## 5. Build / run the image locally (optional)

Docker and Node are already on this box, so you can validate the exact artifact before pushing:

```bash
docker build -t onboarding-platform:local .
docker run --rm -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/onboarding" \
  -e DB_USER=postgres -e DB_PASSWORD=postgres \
  -e JWT_SECRET="local-dev-secret-please-change-32chars" \
  -e AI_PROVIDER=free \
  onboarding-platform:local
# open http://localhost:8080/
```

---

## Known limitations (acceptable for a pilot)

- **Ephemeral disk:** uploaded original files live on the container's local disk and are **lost on
  redeploy/restart**. RAG is unaffected (chunks + embeddings are in Postgres); only the *download
  original file* action breaks for pre-restart uploads. For durability later, attach a persistent
  disk (Render Disks / Fly Volumes) or move storage to S3-compatible object storage and point
  `STORAGE_DIR` / the storage layer at it.
- **Cold starts:** free instances sleep when idle. Keep one warm by pinging `/health` on a
  schedule if testers hit latency.
- **Stub RAG quality:** `AI_PROVIDER=free` gives rough, extractive answers. Switch to `paid`
  (+ keys) and **re-ingest documents** for real quality (embeddings aren't comparable across
  providers).
