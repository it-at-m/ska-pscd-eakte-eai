ALTER TABLE pscdeakte.pscd_import
    RENAME COLUMN created_at TO created_time;

ALTER TABLE pscdeakte.pscd_import
    RENAME COLUMN updated_at TO updated_time;

ALTER TABLE pscdeakte.pscd_import
    ALTER COLUMN updated_time TYPE TIMESTAMP USING updated_time AT TIME ZONE 'Europe/Berlin';

UPDATE pscdeakte.pscd_import
SET created_time = CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Berlin'
WHERE created_time IS NULL;

ALTER TABLE pscdeakte.pscd_import
    ALTER COLUMN created_time SET NOT NULL;

ALTER TABLE pscdeakte.logs
    RENAME COLUMN created_at TO created_time;

ALTER TABLE pscdeakte.logs
    ADD COLUMN updated_time TIMESTAMP;

UPDATE pscdeakte.logs
SET created_time = CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Berlin'
WHERE created_time IS NULL;

UPDATE pscdeakte.logs
SET updated_time = created_time
WHERE updated_time IS NULL;

ALTER TABLE pscdeakte.logs
    ALTER COLUMN created_time SET NOT NULL,
    ALTER COLUMN updated_time SET NOT NULL;
