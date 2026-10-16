CREATE TABLE pscd_import
(
    id                   SERIAL PRIMARY KEY,
    geschaeftspartner_id VARCHAR(10) UNIQUE,
    name                 VARCHAR(255),
    vorname              VARCHAR(255),
    geburtsdatum         VARCHAR(10),
    zentralakt           VARCHAR(50),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL,
    betreffseinheit      VARCHAR(30),
    akte                 VARCHAR(30),
    bestandsakt          VARCHAR(30),
    av                   VARCHAR(30),
    status               VARCHAR(255)
);

CREATE TABLE logs
(
    id         SERIAL PRIMARY KEY,
    level      VARCHAR(5),
    message    VARCHAR(255),
    exception  VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
