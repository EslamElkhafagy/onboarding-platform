# AI Onboarding Platform

Spring Boot backend for the AI-powered employee onboarding platform.
**This build covers Sprint 1 cards 1–4:** deployed skeleton, full DB schema, auth, and new-hire invites.

## Prerequisites
- Java 21
- Maven 3.8+ (or use your IDE's bundled Maven)
- Docker (for local Postgres + pgvector)

## Run it

**1. Start Postgres (with pgvector):**
```bash
docker compose up -d
```

**2. Run the app:**
```bash
mvn spring-boot:run
```
On first start, Flyway runs all four migrations (V1–V4) automatically, creating the full schema.

**3. Verify it's up:**
```bash
curl http://localhost:8080/health
# {"status":"UP"}
```

## Try the auth flow

**Register a company + first admin:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"companyName":"Acme Inc","fullName":"Sara Ali","email":"sara@acme.com","password":"password123"}'
```
Returns a JWT. Save it:
```bash
TOKEN="<paste token>"
```

**Get your profile:**
```bash
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

**Invite a new hire (admin only):**
```bash
curl -X POST http://localhost:8080/api/auth/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"newhire@acme.com","fullName":"Omar Said","startDate":"2026-07-01"}'
```

**Log in:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sara@acme.com","password":"password123"}'
```

## Configuration
Override via environment variables (see `application.yml`):
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET` (set a long random value in production — min 32 chars)
- `PORT`

## Project layout
```
src/main/java/com/onboarding/
  OnboardingPlatformApplication.java
  config/        SecurityConfig, error handling
  controller/    HealthController, AuthController
  dto/           request/response records
  entity/        Company, User, Role
  repository/    CompanyRepository, UserRepository (tenant-scoped finders)
  security/      JwtService, JwtAuthFilter, AuthPrincipal
  service/       AuthService
src/main/resources/
  application.yml
  db/migration/  V1–V4 Flyway migrations (full MVP schema)
```

## What's next (Sprint 2)
Document upload + ingestion: `DocumentController`, file storage, text extraction
(PDFBox/POI), chunking, and embedding generation into `document_chunks`.

## Note on tenant isolation
Every authenticated request carries the user's `companyId` in the JWT, surfaced as
`AuthPrincipal`. Repositories expose `...AndCompanyId` finders so all data access can be
scoped to the caller's company. Keep this discipline as you add features.
