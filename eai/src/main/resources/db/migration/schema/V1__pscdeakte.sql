CREATE SCHEMA IF NOT EXISTS pscdeakte;

CREATE TABLE pscdeakte.pscd_import
(

    id                   SERIAL PRIMARY KEY,    -- INT PK
    geschaeftspartner_id    VARCHAR(10),
    name                    VARCHAR(50),
    vorname                 VARCHAR(50),
    geburtsdatum            VARCHAR(8),
    zentralakt              VARCHAR(50),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL

);

