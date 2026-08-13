--liquibase formatted sql

--changeset gabriel-gerhardt:004-add-section-sort-order
-- Section order was implicit in the primary key: `replaceSections` deleted
-- every row and re-inserted, so ORDER BY id happened to reproduce the
-- submitted order. That coupled ordering to insert order and reset ids on
-- every save. An explicit sort column makes order first-class and lets rows
-- be updated in place instead of recreated.
-- Named `sort_order` rather than `position`, which is a SQL keyword.
ALTER TABLE sections ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

UPDATE sections s
SET sort_order = ranked.rn - 1
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY qrcode_id ORDER BY id) AS rn
    FROM sections
) ranked
WHERE s.id = ranked.id;

CREATE INDEX idx_sections_qrcode_id_sort_order
    ON sections(qrcode_id, sort_order);
