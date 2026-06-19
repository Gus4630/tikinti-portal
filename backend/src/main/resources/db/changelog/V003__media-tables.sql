-- ============================================================
-- V003 — Media tables for buildings and expenses
-- ============================================================

-- Building media: progress photos and walkthrough videos per property
CREATE TABLE building_media (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id   UUID          NOT NULL REFERENCES buildings(id),
    url           VARCHAR(1000) NOT NULL,
    media_type    VARCHAR(10)   NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
    caption       VARCHAR(300),
    display_order INT           NOT NULL DEFAULT 0,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    UUID,
    updated_by    UUID
);

CREATE INDEX idx_building_media_building_id ON building_media(building_id);

-- Expense media: additional work photos beyond the primary invoice scan
-- (invoice image stays in expenses.image_url for OCR pipeline)
CREATE TABLE expense_media (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id    UUID          NOT NULL REFERENCES expenses(id),
    url           VARCHAR(1000) NOT NULL,
    media_type    VARCHAR(10)   NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
    caption       VARCHAR(300),
    display_order INT           NOT NULL DEFAULT 0,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    UUID,
    updated_by    UUID
);

CREATE INDEX idx_expense_media_expense_id ON expense_media(expense_id);
