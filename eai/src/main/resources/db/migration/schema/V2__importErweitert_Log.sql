CREATE SCHEMA IF NOT EXISTS pscdeakte;


ALTER TABLE pscdeakte.pscd_import
    ALTER COLUMN geburtsdatum TYPE VARCHAR(10),
    ADD COLUMN betreffseinheit VARCHAR(30),
    ADD COLUMN akte            VARCHAR(30),
    ADD COLUMN bestandsakt     VARCHAR(30),
    ADD COLUMN av              VARCHAR(30),
    ADD COLUMN status          SMALLINT,
    ADD COLUMN statustext      VARCHAR(50);

CREATE TABLE pscdeakte.logs
(

    id                   SERIAL PRIMARY KEY,    -- INT PK
    level                   VARCHAR(5),
    message                 VARCHAR(50),
    exception               VARCHAR(100),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

