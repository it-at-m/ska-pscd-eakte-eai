package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import de.muenchen.oss.pscdeakte.database.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DBLogger {

    private final LogEntryRepository logEntryRepo;

    @Async
    public void log(final Level level, final String message, final String exception, final String gpId) {
        final LogEntry logEntry = new LogEntry();
        logEntry.setLevel(level.toString());
        logEntry.setMessage(message);
        logEntry.setException(exception);
        logEntry.setGpId(gpId);

        logEntryRepo.save(logEntry);
    }

    public void info(final String message, final String exception, final String gpId) {
        this.log(Level.INFO, message, exception, gpId);
    }

    public void error(final String message, final String exception, final String gpId) {
        this.log(Level.ERROR, message, exception, gpId);
    }
}
