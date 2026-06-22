-- ============================================================
-- V006 — Expense line items
-- ============================================================

CREATE TABLE expense_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id  UUID         NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity    INT          NOT NULL DEFAULT 1,
    unit_price  NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID
);

CREATE INDEX idx_expense_items_expense_id ON expense_items(expense_id);
