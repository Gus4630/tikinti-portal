--liquibase formatted sql
--changeset tikinti:4

ALTER TABLE expenses ALTER COLUMN category_id DROP NOT NULL;
