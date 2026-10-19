ALTER TABLE pscdeakte.pscd_import
    DROP CONSTRAINT pscd_import_pkey,
    DROP CONSTRAINT geschaeftspartner_id,
    DROP COLUMN id,
    ADD PRIMARY KEY (geschaeftspartner_id);
