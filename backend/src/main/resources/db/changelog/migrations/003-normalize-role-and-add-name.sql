--liquibase formatted sql

--changeset gabriel-gerhardt:003-normalize-role-to-string
-- The `role` column is VARCHAR, but the entity mapped the enum without
-- @Enumerated(EnumType.STRING), so Hibernate persisted the ORDINAL and the
-- column ended up holding "0"/"1" instead of "USER"/"ADMIN". Normalizing the
-- stored values here so the column is readable/queryable by name, and so the
-- data survives adding a new constant to the enum (which would otherwise
-- silently re-map every existing row's ordinal).
UPDATE users SET role = 'USER' WHERE role = '0';
UPDATE users SET role = 'ADMIN' WHERE role = '1';

--changeset gabriel-gerhardt:003-add-user-name
-- The design's Create Account screen collects "Nome" and Profile renders it
-- next to the email; `username` alone cannot serve both. Nullable so existing
-- rows stay valid and the field remains optional at registration.
ALTER TABLE users ADD COLUMN name VARCHAR(255);
