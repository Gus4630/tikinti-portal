# Tikinti Portal

Construction expense management platform for multi-building projects. Handles invoice
OCR processing, expense tracking, budget monitoring, supplier management, and financial
reporting — with Telegram alerts for budget thresholds and expense review workflows.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5, Java 21, Spring Security, Spring Data JPA |
| Frontend | Nuxt 4, Vue 3 (Composition API), Pinia, Nuxt UI |
| Database | PostgreSQL 16 with Liquibase migrations |
| Queue | RabbitMQ 3 (async OCR job processing) |
| OCR | Google Gemini API (primary + fallback model) |
| FX Rates | CBAR (Central Bank of Azerbaijan) public API |
| Proxy | Nginx (TLS termination, SPA serving, API proxy) |
| Notifications | Telegram Bot API |

---

## Architecture

```
Browser
  └── Nginx (443)
        ├── /           → Static SPA files (Nuxt build output)
        └── /api/       → Spring Boot backend (port 8080, internal)
                              ├── PostgreSQL
                              └── RabbitMQ → InvoiceProcessingWorker → Gemini API
```

The frontend is a **pure SPA** (`ssr: false`) — `nuxt build` produces static files
served directly by Nginx with no Node.js process at runtime.

The backend is a **fat JAR** built with Gradle, containerised via a two-stage Dockerfile
(`eclipse-temurin:21-jdk-alpine` builder → `eclipse-temurin:21-jre-alpine` runtime).

---

## Local Development

### Prerequisites

- Java 21
- Node.js 22, pnpm
- Docker + Docker Compose

### 1. Start infrastructure (PostgreSQL + RabbitMQ)

```bash
cp infra.env.example infra.env   # fill in passwords
docker compose up postgres rabbitmq -d
```

### 2. Backend

```bash
cp backend/.env.example backend/.env   # fill in values
cd backend
./gradlew bootRun
# Runs on http://localhost:8080
# Liquibase migrations run automatically on startup
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 3. Frontend

```bash
cd frontend
pnpm install
pnpm dev
# Runs on http://localhost:3000
```

---

## Project Structure

```
tikinti-portal/
├── backend/
│   ├── src/main/java/az/tikinti/portal/
│   │   ├── config/          # Security, OpenAPI, CORS
│   │   ├── controller/      # REST controllers
│   │   ├── dao/
│   │   │   ├── entity/      # JPA entities
│   │   │   ├── repository/  # Spring Data repositories
│   │   │   └── specification/
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── model/dto/       # Request / Response DTOs
│   │   ├── service/         # Business logic
│   │   └── util/
│   └── src/main/resources/
│       ├── db/changelog/    # Liquibase migrations (V001–V012)
│       └── application.yml
├── frontend/
│   └── app/
│       ├── components/      # CategoryPicker, ManualExpenseModal, etc.
│       ├── composables/
│       ├── layouts/
│       ├── pages/
│       │   ├── expenses/    # List + detail
│       │   ├── buildings/   # Project management + budget limits
│       │   ├── categories/  # Three-level category hierarchy
│       │   ├── reports/
│       │   └── suppliers/
│       ├── plugins/
│       └── stores/          # auth, context (active building)
├── infra/
│   └── nginx/               # nginx.conf + TLS certs (create before deploying)
├── docker-compose.yml
├── infra.env.example
└── DEPLOYMENT.md            # Full production deployment guide (gitignored)
```

---

## Key Features

- **Invoice OCR** — upload a PDF/image invoice; Gemini extracts supplier, amount,
  date, and line items automatically via an async RabbitMQ job
- **Manual expense entry** — bottom-sheet UI with category picker, line items,
  draft persistence (sessionStorage), and auto-calculated totals
- **Per-building budget limits** — set category budget caps per project; alerts fire
  at 80% and 100% thresholds via Telegram
- **Multi-currency** — expenses recorded in original currency, converted to AZN base
  using live CBAR exchange rates
- **Reports** — budget vs actual, monthly trend, supplier ledger, spend forecast,
  cost per m², disputed expenses; Excel export
- **Group / multi-tenant** — users belong to groups; buildings are scoped to a group
- **Audit trail** — Hibernate Envers tracks all entity changes

---

## Environment Variables

See `infra.env.example` and `backend/.env.example` for the full list.
Critical variables:

| Variable | Where | Purpose |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `backend/.env` | PostgreSQL connection |
| `RABBIT_USERNAME` / `RABBIT_PASSWORD` | `backend/.env` | RabbitMQ credentials |
| `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` | `backend/.env` | Token signing (min 32 chars) |
| `GEMINI_API_KEY` | `backend/.env` | Google AI Studio key for OCR |
| `GEMINI_PRIMARY_MODEL` / `GEMINI_FALLBACK_MODEL` | `backend/.env` | e.g. `gemini-3.1-flash-lite` |
| `NUXT_PUBLIC_API_BASE` | build-time env | Backend URL the browser calls |
| `CORS_ALLOWED_ORIGINS` | `backend/.env` | Comma-separated allowed frontend origins |

---

## API

Spring Boot exposes REST at `/api/v1/`. Swagger UI is available at
`http://localhost:8080/swagger-ui.html` in development (disabled in production).

Public endpoints (no auth): `/api/v1/auth/**`, `/api/v1/public/**`, `/actuator/health`.

Authentication: JWT Bearer tokens. Access token TTL: 15 min. Refresh token TTL: 30 days.

---

## Deployment

See `DEPLOYMENT.md` (kept local, gitignored) for the full production guide including
provider selection, server hardening, Nginx TLS config, and a step-by-step deployment
sequence.

Quick summary:
```bash
docker compose up -d --build   # starts postgres, rabbitmq, backend, nginx
```

Requires `infra.env`, `backend/.env`, `infra/nginx/nginx.conf`, and TLS certificates
in `infra/nginx/certs/` to be present on the server before starting.
