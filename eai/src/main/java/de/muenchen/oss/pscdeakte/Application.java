package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

@ConfigurationPropertiesScan
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.UseUtilityClass")
public class Application {
    private final CsvToDb csvToDb;
    private final DbToEakte dbToEakte;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Scheduled(cron = "${dms.cron}")
    @LogExecutionTime
    protected void scheduledTask() throws S3Exception {
        if (STILL_RUNNING.get()) {
            log.info("Nothing to do, scheduled task still running");
        } else {
            log.info("starting");
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
