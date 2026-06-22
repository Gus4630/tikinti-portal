# Tikinti Portal — Project Structure

Expense management portal for construction projects.
Spring Boot 3 backend + Nuxt 4 frontend, communicating via REST.

---

## Repository Layout

```
tikinti-portal/
├── backend/          Spring Boot 3.5 (Java 21, Gradle)
├── frontend/         Nuxt 4 (Vue 3, TypeScript, pnpm)
└── PROJECT_STRUCTURE.md
```

---

## Backend

### Stack

| Concern | Technology |
|---|---|
| Framework | Spring Boot 3.5 |
| Language | Java 21 |
| Build | Gradle (Groovy DSL) |
| Persistence | Spring Data JPA + Hibernate Envers (audit) |
| DB migrations | Liquibase (SQL changesets) |
| Database | PostgreSQL |
| Messaging | RabbitMQ (Spring AMQP) |
| Object mapping | MapStruct 1.6 |
| Boilerplate | Lombok |
| Security | Spring Security + JWT (jjwt 0.12) |
| API docs | SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`) |
| HTTP client | OpenFeign (OkHttp transport) |
| OCR / AI | Google Gemini (REST via Feign) |
| Observability | Logback + Logstash JSON encoder, Spring Actuator |
| Reporting | EasyExcel |

### Package Tree

```
az.tikinti.portal/
│
├── TikintiPortalApplication.java
│
├── aop/
│   └── LoggingAspect.java              Method-level structured logging via @Around
│
├── client/
│   └── cbar/                           Feign client for CBAR (Central Bank) FX rates
│       ├── CbarClient.java
│       └── model/
│           ├── CbarCurrency.java
│           ├── CbarCurrencyResponse.java
│           └── CbarCurrencyType.java
│
├── config/
│   ├── AppConfig.java                  Bean definitions (ObjectMapper, etc.)
│   ├── JpaAuditingConfig.java          Enables @CreatedBy / @LastModifiedBy
│   ├── JwtAuthenticationFilter.java    Servlet filter — validates Bearer token
│   ├── OpenApiConfig.java              Swagger security scheme
│   ├── RabbitConfig.java               Queue / exchange declarations
│   └── SecurityConfig.java             Security filter chain, CORS, public routes
│
├── controller/
│   ├── auth/         AuthController.java
│   ├── building/     BuildingController.java
│   ├── category/     CategoryController.java
│   ├── expense/      ExpenseController.java
│   ├── file/         FileController.java
│   ├── group/        GroupController.java
│   ├── invoice/      InvoiceUploadController.java
│   ├── report/       ReportController.java
│   └── supplier/     SupplierController.java
│
├── dao/
│   ├── entity/
│   │   ├── BaseEntity.java             UUID PK, isActive, createdAt/By, updatedAt/By
│   │   ├── auth/
│   │   │   ├── RefreshTokenEntity.java
│   │   │   └── UserEntity.java
│   │   ├── building/
│   │   │   ├── BuildingEntity.java
│   │   │   └── BuildingMediaEntity.java
│   │   ├── category/
│   │   │   └── CategoryEntity.java     itemCode, level1/2/3, itemName, budgetLimit
│   │   ├── expense/
│   │   │   ├── ExpenseEntity.java      Core entity; holds amount, status, notes, items
│   │   │   ├── ExpenseItemEntity.java  OCR-extracted line items (build expenses)
│   │   │   └── ExpenseMediaEntity.java Attached files / images
│   │   ├── fx/
│   │   │   └── CurrencyRateEntity.java Daily FX rate cache from CBAR
│   │   ├── group/
│   │   │   ├── GroupEntity.java
│   │   │   └── GroupMemberEntity.java
│   │   └── supplier/
│   │       └── SupplierEntity.java
│   │
│   ├── repository/                     Spring Data JPA repositories (one per entity)
│   │   ├── auth/
│   │   ├── building/
│   │   ├── category/
│   │   ├── expense/
│   │   │   ├── ExpenseItemRepository.java
│   │   │   ├── ExpenseMediaRepository.java
│   │   │   └── ExpenseRepository.java
│   │   ├── fx/
│   │   ├── group/
│   │   └── supplier/
│   │
│   └── specification/                  JPA Criteria specs for filtered search endpoints
│       ├── BaseSpecification.java      equalPredicate / likePredicate / rangePredicate / inPredicate helpers
│       ├── BuildingSpecification.java
│       ├── CategorySpecification.java
│       ├── ExpenseSpecification.java
│       └── SupplierSpecification.java
│
├── event/
│   ├── BudgetThresholdEvent.java       Fired when category spend hits 80% or 100% of budget
│   ├── ExpenseDisputedEvent.java       Fired on dispute / anomaly detection
│   └── ExpensePendingReviewEvent.java  Fired after successful OCR
│
├── exception/
│   ├── AlreadyExistsException.java
│   ├── CommonException.java            Base exception — carries ErrorCode + message args
│   ├── DataNotFoundException.java
│   ├── GlobalExceptionHandler.java     @RestControllerAdvice → ErrorResponse
│   ├── NonRetryableOcrException.java   Signals RabbitMQ dead-letter (no retry)
│   └── model/constant/
│       ├── ErrorCode.java              DATA_NOT_FOUND, ALREADY_EXIST, …
│       └── ErrorMessage.java           MessageFormat templates, statically imported in services
│
├── mapper/                             MapStruct interfaces (one sub-package per domain)
│   ├── building/  BuildingMapper.java
│   ├── category/  CategoryMapper.java
│   ├── expense/
│   │   ├── ExpenseItemMapper.java      OcrExtractionResult.LineItem + ExpenseEntity → ExpenseItemEntity
│   │   └── ExpenseMapper.java
│   ├── fx/        CurrencyRateMapper.java
│   ├── group/     GroupMapper.java
│   └── supplier/  SupplierMapper.java
│
├── model/
│   ├── constant/
│   │   └── CbarConstants.java          BASE_CURRENCY, date formatter, rounding — statically imported
│   ├── dto/
│   │   ├── record/                     Immutable Java records (events, OCR results, internal messages)
│   │   │   ├── OcrExtractionResult.java  (+ nested LineItem record)
│   │   │   ├── InvoiceMessage.java
│   │   │   ├── SessionResponse.java
│   │   │   └── …reporting rows…
│   │   ├── request/                    Mutable request DTOs per domain
│   │   │   ├── PageableRequest.java
│   │   │   ├── auth/
│   │   │   ├── building/
│   │   │   ├── category/
│   │   │   ├── expense/
│   │   │   ├── group/
│   │   │   └── supplier/
│   │   └── response/                   Response DTOs per domain (@JsonInclude NON_NULL)
│   │       ├── PageableResponse.java
│   │       ├── auth/
│   │       ├── building/
│   │       ├── category/
│   │       ├── expense/
│   │       │   ├── ExpenseItemResponse.java
│   │       │   ├── ExpenseMediaResponse.java
│   │       │   └── ExpenseResponse.java
│   │       ├── group/
│   │       └── supplier/
│   └── enums/
│       ├── ExpenseStatus.java          UPLOADED → OCR_PROCESSING → PENDING_REVIEW → APPROVED / DISPUTED / REJECTED
│       ├── MediaType.java
│       ├── SupplierType.java
│       └── UserRole.java
│
├── service/
│   ├── auth/         AuthService.java, JwtService.java
│   ├── building/     BuildingService.java
│   ├── category/     CategoryService.java
│   ├── expense/      ExpenseService.java
│   ├── file/         FileStorageService.java
│   ├── fx/           CurrencyRateService.java       CBAR fetch + cache
│   ├── group/        GroupService.java
│   ├── invoice/      InvoiceUploadService.java      Receives upload, creates stub expense, publishes to RabbitMQ
│   ├── notification/ TelegramNotificationService.java
│   ├── ocr/
│   │   ├── AbstractGeminiExtractionService.java    Base Gemini prompt logic
│   │   ├── GeminiFlashLiteExtractionService.java   Primary OCR (gemini-flash-lite)
│   │   ├── GeminiFlashExtractionService.java       Fallback OCR (gemini-flash)
│   │   ├── InvoiceExtractionService.java
│   │   └── InvoiceProcessingWorker.java            RabbitMQ consumer — runs OCR, hydrates expense
│   ├── report/       ReportService.java
│   └── supplier/     SupplierService.java
│
└── util/
    ├── HashUtil.java       SHA-256 for content-hash dedup
    ├── LogUtil.java
    ├── PageUtil.java       PageableRequest → Spring Pageable
    ├── TraceIdFilter.java  MDC trace-id injection
    └── WebUtil.java
```

### Database Migrations (Liquibase)

Located in `src/main/resources/db/changelog/`, included in order by `db.changelog-master.yaml`:

| File | Description |
|---|---|
| `V001__init-schema.sql` | users, refresh_tokens, categories, suppliers, buildings, expenses |
| `V002__seed-categories.sql` | Seed construction category codes |
| `V003__media-tables.sql` | building_media, expense_media |
| `V004__nullable-category.sql` | Makes expense.category_id nullable |
| `V005__groups-and-budget.sql` | groups, group_members, budget_limit on categories |
| `V006__expense-items.sql` | expense_items (OCR-extracted structured line items) |

### Code Conventions (Backend)

**Mappers** — every mapper is an `interface` using these exact static imports:
```java
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
```

**Services** — class-level annotations:
```java
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
```
Mutating methods carry their own `@Transactional`. Mappers are injected as `final` fields alongside repositories.

**Static imports used throughout:**
- `ErrorMessage.*` — message format strings for exceptions
- `ErrorCode.*` — error code constants
- `CbarConstants.*` — BASE_CURRENCY, date formatter, rounding mode
- `ExpenseStatus.*` — enum constants in workers and services
- `SupplierType.*`, `UserRole.*` — enum constants
- `InjectionStrategy.CONSTRUCTOR`, `MappingConstants.ComponentModel.SPRING` — in every mapper
- `HttpStatus.CREATED` — in controllers returning 201
- `BaseSpecification.*` — predicate helpers in Specification classes

**Soft delete** — `entity.setIsActive(false)`, never `repository.delete(...)`.

**404 pattern** — `DataNotFoundException.of(ERROR_MESSAGE_CONSTANT, fieldName, value)`.

**Pagination pipeline** — `PageableRequest` → `PageUtil.createPageable(request)` → `Page<Entity>` → `mapper.toResponse(page)` → `PageableResponse<Response>`.

**Collection fields on responses** (`media`, `items`) — ignored by the mapper (`@Mapping(target = "x", ignore = true)`), loaded separately in the service via repository queries, then set with `response.setX(...)`. This avoids lazy-load issues outside a transaction.

**OCR line item routing** — `InvoiceProcessingWorker.hydrateExpense()`:
- Category matched (build expense confirmed) → line items saved as `ExpenseItemEntity` rows via `ExpenseItemMapper`
- No category (uncertain) → line items flattened into `expense.notes` as free text

---

## Frontend

### Stack

| Concern | Technology |
|---|---|
| Framework | Nuxt 4 (`compatibilityVersion: 4`) |
| Language | TypeScript |
| UI library | Nuxt UI 4 (`@nuxt/ui`) |
| State | Pinia 3 (`@pinia/nuxt`) |
| Package manager | pnpm (workspace) |
| Linting | `@nuxt/eslint` with stylistic config |

### Directory Layout

Nuxt 4 uses the `app/` directory as the source root (replaces `src/`).

```
frontend/
├── app/
│   ├── app.vue                         Root layout shell
│   ├── app.config.ts                   Nuxt UI theme tokens
│   ├── assets/
│   │   └── css/main.css                Global styles; custom classes: tk-card, data-table, badge-*
│   ├── components/
│   │   ├── AddGroupMemberModal.vue
│   │   ├── AppLogo.vue
│   │   ├── BuildingModal.vue
│   │   ├── ExpenseUploadModal.vue
│   │   ├── SupplierModal.vue
│   │   └── TemplateMenu.vue
│   ├── composables/
│   │   └── useApi.ts                   Wraps $fetch; handles auth headers + 401 logout
│   ├── layouts/
│   │   └── default.vue                 Sidebar + top-bar shell for authenticated pages
│   ├── middleware/
│   │   └── auth.ts                     Route guard — redirects unauthenticated users to /login
│   ├── pages/
│   │   ├── index.vue                   Redirects to /dashboard
│   │   ├── login.vue
│   │   ├── dashboard.vue
│   │   ├── settings.vue
│   │   ├── buildings/
│   │   │   └── index.vue
│   │   ├── expenses/
│   │   │   ├── index.vue               Paginated expense list with filters
│   │   │   └── [id].vue                Expense detail + status actions
│   │   ├── groups/
│   │   │   └── index.vue
│   │   ├── reports/
│   │   │   └── index.vue
│   │   └── suppliers/
│   │       └── index.vue
│   ├── plugins/
│   │   └── auth.client.ts              Hydrates auth store from localStorage on client boot
│   └── stores/
│       └── auth.ts                     Pinia store — tokens, user, login/logout
│
├── public/
├── nuxt.config.ts
├── package.json
├── pnpm-lock.yaml
├── tsconfig.json
└── eslint.config.mjs
```

### Frontend Conventions

**Store style** — Composition API (`defineStore(id, () => { ... })`), never Options API.

**Token storage** — `localStorage` with keys `tk_access` / `tk_refresh`, guarded by `if (import.meta.client)`. Hydration happens in `plugins/auth.client.ts` (client-only plugin).

**Data fetching** — `useAsyncData(key, fetcher, { watch: [reactiveFilters] })` for SSR-compatible reactive data; `apiFetch` from `useApi()` composable for mutations.

**`useApi` composable** — returns `{ apiFetch, authHeaders }`. Automatically attaches `Authorization: Bearer …` and handles 401 by calling `auth.logout()` then `navigateTo('/login')`.

**UI** — Nuxt UI's `UIcon` for icons; most table/form UI is plain HTML with custom CSS classes from `main.css`. Heavy use of inline styles.

**Locale** — Azerbaijani throughout (`lang="az"`, UI strings in Azerbaijani, `'az-AZ'` number formatting, `₼` currency symbol).

**ESLint** — `commaDangle: 'never'`, `braceStyle: '1tbs'`.

---

## Environment Configuration

### Backend (`backend/.env.local` for local dev)

```
DB_URL=jdbc:postgresql://localhost:5432/tikinti
DB_USER=postgres
DB_PASSWORD=<your-postgres-password>

JWT_ACCESS_SECRET=<64+ char secret>
JWT_REFRESH_SECRET=<64+ char secret>
JWT_ACCESS_EXPIRY_MINUTES=15
JWT_REFRESH_EXPIRY_DAYS=30

GEMINI_API_KEY=<key>
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta

RABBIT_HOST=localhost
RABBIT_PORT=5672
RABBIT_USERNAME=guest
RABBIT_PASSWORD=guest

CBAR_BASE_URL=https://cbar.az
UPLOAD_DIR=/tmp/tikinti-uploads
```

Loaded automatically by the `bootRun` Gradle task. Production uses `backend/.env` (Docker hostnames).

### Frontend (`frontend/.env.example`)

```
NUXT_PUBLIC_API_BASE=http://localhost:8080
```

---

## Key Flows

### Invoice OCR Flow

1. `POST /api/v1/invoices/upload` (multipart) → `InvoiceUploadController`
2. `InvoiceUploadService` creates stub `ExpenseEntity` (status=`UPLOADED`) and publishes `InvoiceMessage` to RabbitMQ
3. `InvoiceProcessingWorker` consumes the message:
   - Sets status → `OCR_PROCESSING`
   - Reads file bytes, calls `GeminiFlashLiteExtractionService` (primary)
   - Falls back to `GeminiFlashExtractionService` if confidence < 0.6
   - `hydrateExpense()` populates: amount, date, currency (+ CBAR FX), category, supplier, line items
   - **Line item routing:** category matched → saved as `ExpenseItemEntity` rows; no category → saved as `expense.notes` text
   - Anomaly check (MAD-based): outlier amounts → status=`DISPUTED`
   - Final status → `PENDING_REVIEW`
4. Spring event published → `TelegramNotificationService` sends alert

### Expense Status Lifecycle

```
UPLOADED → OCR_PROCESSING → PENDING_REVIEW → APPROVED
                                          ↘ DISPUTED
                                          ↘ REJECTED
```

### Pagination Pattern

All list endpoints follow:
```
GET /api/v1/expenses?page=0&size=20&sort=createdAt&direction=DESC&...filters
```
Backed by `PageableRequest` → `PageUtil.createPageable()` → JPA Specification → `PageableResponse<T>`.
