--liquibase formatted sql

--changeset tikinti:V009-1 comment:Replace unconditional unique constraint with partial index (only active members)
ALTER TABLE group_members DROP CONSTRAINT uq_group_member;

--changeset tikinti:V009-2 comment:Partial unique index so removed members can be re-added
CREATE UNIQUE INDEX uq_group_member ON group_members(group_id, user_id) WHERE is_active = TRUE;
