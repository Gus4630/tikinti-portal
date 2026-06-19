--liquibase formatted sql
--changeset tikinti:5

ALTER TABLE users ADD COLUMN full_name VARCHAR(200);

ALTER TABLE buildings ADD COLUMN budget_limit NUMERIC(19,2);

CREATE TABLE groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID
);

CREATE TABLE group_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    UUID         NOT NULL REFERENCES groups(id),
    user_id     UUID         NOT NULL REFERENCES users(id),
    member_role VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);
CREATE INDEX idx_group_members_group_id ON group_members(group_id);

ALTER TABLE buildings ADD COLUMN group_id UUID REFERENCES groups(id);
CREATE INDEX idx_buildings_group_id ON buildings(group_id);
