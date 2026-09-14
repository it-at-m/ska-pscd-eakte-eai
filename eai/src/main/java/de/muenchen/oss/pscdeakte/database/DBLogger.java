package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DBLogger {
    public static final int DB_FIELD_LENGTH = 255;
    @PersistenceContext
    private EntityManager entityManager;

    @Async
    @Transactional
    public void log(final String level, final String message, final String exception) {
        final LogEntry logEntry = new LogEntry();
        logEntry.setLevel(level);
        logEntry.setMessage(this.limitLength(message));
        logEntry.setException(this.limitLength(exception));

        entityManager.persist(logEntry);
    }

    private String limitLength(final String log) {
        if (log == null) {
            return null;
        }
        return log.length() > DB_FIELD_LENGTH ? log.substring(0, DB_FIELD_LENGTH) : log;
    }
}
