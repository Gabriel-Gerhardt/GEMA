--liquibase formatted sql

--changeset gabriel-gerhardt:005-add-plan-owner-name
-- The Emergency Guide's headline greets the reader in the plan subject's own
-- voice ("Olá, meu nome é Lucas."), but no column held that name, so the
-- frontend could only ever show it from seed data.
--
-- It lives on the plan rather than the account because one account may hold
-- plans for more than one person — a caregiver making a plan for someone they
-- support is the case the product is built around.
ALTER TABLE qrcodes ADD COLUMN owner_name VARCHAR(255);

--changeset gabriel-gerhardt:005-add-plan-emergency-contact
-- The emergency contact was only ever free text inside a section, so the
-- frontend recovered a phone number by running a regex over prose. On the one
-- screen whose entire purpose is reaching a person in a crisis, that is too
-- fragile: reformatting the sentence silently disabled the call button.
ALTER TABLE qrcodes ADD COLUMN emergency_contact_name VARCHAR(255);
ALTER TABLE qrcodes ADD COLUMN emergency_contact_phone VARCHAR(40);
