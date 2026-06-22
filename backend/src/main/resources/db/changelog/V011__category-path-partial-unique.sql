-- Replace full unique constraints with partial ones scoped to active rows only.
-- Soft-deleted categories must not block re-creation of the same path or code.

-- 1. Path uniqueness (named constraint)
ALTER TABLE categories DROP CONSTRAINT uq_category_path;

CREATE UNIQUE INDEX uq_category_path_active
    ON categories (level_1, level_2, level_3, item_name)
    WHERE is_active = TRUE;

-- 2. item_code uniqueness (inline UNIQUE → find its system-generated constraint name and drop it)
DO $$
DECLARE
    cname TEXT;
BEGIN
    SELECT conname INTO cname
    FROM pg_constraint
    WHERE conrelid = 'categories'::regclass
      AND contype = 'u'
      AND pg_get_constraintdef(oid) LIKE '%item_code%';
    IF cname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE categories DROP CONSTRAINT %I', cname);
    END IF;
END $$;

CREATE UNIQUE INDEX uq_category_item_code_active
    ON categories (item_code)
    WHERE is_active = TRUE;
