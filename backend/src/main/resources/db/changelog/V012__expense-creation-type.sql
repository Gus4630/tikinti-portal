-- Add creation_type to distinguish manual vs OCR-uploaded expenses.
-- Existing rows (all OCR-uploaded) default to 'OCR'.
ALTER TABLE expenses
    ADD COLUMN creation_type VARCHAR(20) NOT NULL DEFAULT 'OCR';
