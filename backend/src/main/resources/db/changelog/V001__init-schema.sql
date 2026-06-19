-- ============================================================
-- V001 — Initial schema
-- ============================================================

-- Users
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(200) NOT NULL UNIQUE,
    password     VARCHAR(200) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_by   UUID
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_hash   VARCHAR(64)  NOT NULL,
    device_label VARCHAR(100),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_by   UUID
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Categories
CREATE TABLE categories (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code        VARCHAR(40)  NOT NULL UNIQUE,
    level_1          VARCHAR(100) NOT NULL,
    level_2          VARCHAR(100) NOT NULL,
    level_3          VARCHAR(100) NOT NULL,
    item_name        VARCHAR(200) NOT NULL,
    item_description TEXT,
    budget_limit     NUMERIC(14,2),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    CONSTRAINT uq_category_path UNIQUE (level_1, level_2, level_3, item_name)
);

-- Suppliers
CREATE TABLE suppliers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(200) NOT NULL,
    supplier_type         VARCHAR(20)  NOT NULL CHECK (supplier_type IN ('INDIVIDUAL','COMPANY')),
    tax_id                VARCHAR(20),
    national_id           VARCHAR(20),
    phone_number          VARCHAR(20),
    total_advanced_paid   NUMERIC(14,2) NOT NULL DEFAULT 0,
    retainage_percentage  NUMERIC(5,2)  NOT NULL DEFAULT 0,
    retainage_held_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID
);

-- Buildings (one row per construction project / property)
CREATE TABLE buildings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(200)   NOT NULL,
    address        TEXT,
    description    TEXT,
    floor_area_m2  NUMERIC(10,2),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID
);

-- Expenses
CREATE TABLE expenses (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id          UUID          NOT NULL REFERENCES buildings(id),
    category_id          UUID          NOT NULL REFERENCES categories(id),
    supplier_id          UUID          REFERENCES suppliers(id),
    amount               NUMERIC(14,2) NOT NULL,
    currency             VARCHAR(3)    NOT NULL,
    exchange_rate        NUMERIC(14,6) NOT NULL DEFAULT 1,
    amount_base_currency NUMERIC(14,2) NOT NULL,
    content_hash         VARCHAR(64)   NOT NULL,
    image_url            VARCHAR(500),
    status               VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    notes                TEXT,
    expense_date         DATE          NOT NULL DEFAULT CURRENT_DATE,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_by           UUID
);

CREATE INDEX idx_expenses_building_id ON expenses(building_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_supplier_id ON expenses(supplier_id);
CREATE INDEX idx_expenses_status ON expenses(status);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE UNIQUE INDEX uq_expenses_content_hash ON expenses(content_hash);

-- Currency rates (CBAR cache)
CREATE TABLE currency_rates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exchange_date DATE         NOT NULL,
    currency_code VARCHAR(3)   NOT NULL,
    currency_name VARCHAR(100) NOT NULL,
    nominal       VARCHAR(10)  NOT NULL,
    exchange_rate NUMERIC(14,6) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID,
    updated_by    UUID,
    CONSTRAINT uq_currency_rate_date_code UNIQUE (exchange_date, currency_code)
);

CREATE INDEX idx_currency_rates_date ON currency_rates(exchange_date);

-- Envers audit tables are created automatically by Hibernate
