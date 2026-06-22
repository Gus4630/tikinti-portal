CREATE TABLE building_category_budgets (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id  UUID          NOT NULL REFERENCES buildings(id),
    category_id  UUID          NOT NULL REFERENCES categories(id),
    budget_limit NUMERIC(14,2) NOT NULL CHECK (budget_limit > 0),
    notes        TEXT,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_by   UUID
);

CREATE UNIQUE INDEX uq_building_category_budget_active
    ON building_category_budgets (building_id, category_id)
    WHERE is_active = TRUE;

CREATE INDEX idx_bcb_building_id ON building_category_budgets (building_id);
CREATE INDEX idx_bcb_category_id ON building_category_budgets (category_id);

ALTER TABLE categories DROP COLUMN budget_limit;
