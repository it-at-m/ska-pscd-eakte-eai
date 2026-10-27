DROP INDEX pscdeakte.idx_pscd_import_status;

CREATE INDEX idx_pscd_import_open_created_time
    ON pscdeakte.pscd_import (created_time)
    WHERE status <> 'DONE';
