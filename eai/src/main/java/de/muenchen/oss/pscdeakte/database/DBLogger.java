package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DBLogger {
    @PersistenceContext
    private EntityManager entityManager;

    @Async
    @Transactional
    public void log(final String level, final String message, final String exception) {
        LogEntry logEntry = new LogEntry();
        logEntry.setLevel(level);
        logEntry.setMessage(message);
        logEntry.setException(exception);

        entityManager.persist(logEntry);
    }
}