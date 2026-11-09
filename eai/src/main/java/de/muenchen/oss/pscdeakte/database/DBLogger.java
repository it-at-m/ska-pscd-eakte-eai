package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DBLogger {
    @PersistenceContext
    private EntityManager entityManager;

    @Async
    public void log(final DBLogEntry entry) {
        final LogEntry logEntry = new LogEntry();
        logEntry.setLevel(entry.level());
        logEntry.setMessage(entry.message());
        logEntry.setException(entry.exception());
        logEntry.setGpId(entry.gpId());

        entityManager.persist(logEntry);
    }
}
