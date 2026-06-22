--liquibase formatted sql

--changeset tikinti:V007-1 comment:Deactivate orphaned buildings that have no group assigned
UPDATE buildings
SET is_active = false,
    updated_at = now()
WHERE group_id IS NULL;

--changeset tikinti:V007-2 comment:Enforce NOT NULL on buildings.group_id
ALTER TABLE buildings ALTER COLUMN group_id SET NOT NULL;
