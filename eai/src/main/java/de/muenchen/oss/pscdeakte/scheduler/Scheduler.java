package de.muenchen.oss.pscdeakte.scheduler;

import de.muenchen.oss.pscdeakte.CsvToDb;
import de.muenchen.oss.pscdeakte.DbToEakte;
import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

    private final CsvToDb csvToDb;
    private final DbToEakte dbToEakte;

    private static final AtomicBoolean STILL_RUNNING = new AtomicBoolean(false);

    @Scheduled(cron = "${dms.cron}")
    @LogExecutionTime
    protected void scheduledTask() throws S3Exception {
        if (STILL_RUNNING.get()) {
            log.info("Nothing to do, scheduled task still running");
        } else {
            STILL_RUNNING.set(true);
            try {
                this.csvToDb.processFiles();
                this.dbToEakte.start();
            } finally {
                STILL_RUNNING.set(false);
            }
        }
    }
}
