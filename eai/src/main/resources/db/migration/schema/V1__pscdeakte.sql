CREATE SCHEMA IF NOT EXISTS pscdeakte;

CREATE TABLE pscdeakte.pscd_import
(

    id                   SERIAL PRIMARY KEY,    -- INT PK
    geschaeftspartner_id    VARCHAR(10),
    name                    VARCHAR(50),
    vorname                 VARCHAR(50),
    geburtsdatum            VARCHAR(10),
    zentralakt              VARCHAR(4),
    betreffseinheit         VARCHAR(30),
    akte                    VARCHAR(30),
    bestandsakt             VARCHAR(30),
    av                      VARCHAR(30),
    status                  SMALLINT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL

);

CREATE TABLE pscdeakte.logs
(

    id                   SERIAL PRIMARY KEY,    -- INT PK
    level                   VARCHAR(5),
    message                 VARCHAR(50),
    exception               VARCHAR(100),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

