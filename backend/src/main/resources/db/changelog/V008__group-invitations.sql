--liquibase formatted sql

--changeset tikinti:V008-1 comment:Create group_invitations table
CREATE TABLE group_invitations (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID        NOT NULL REFERENCES groups(id),
    invited_user_id UUID        NOT NULL REFERENCES users(id),
    invited_by_id   UUID        NOT NULL REFERENCES users(id),
    member_role     VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

--changeset tikinti:V008-2 comment:Unique index so only one pending invitation exists per group+user pair
CREATE UNIQUE INDEX uq_pending_invitation
    ON group_invitations(group_id, invited_user_id)
    WHERE status = 'PENDING' AND is_active = TRUE;

--changeset tikinti:V008-3 comment:Index for fetching a user's invitations
CREATE INDEX idx_invitations_invited_user ON group_invitations(invited_user_id);
