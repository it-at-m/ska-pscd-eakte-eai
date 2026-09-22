package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@ConfigurationPropertiesScan
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class Application {
    private final CsvToDb csvToDb;
    private final DbToEakte dbToEakte;

    private static final AtomicBoolean stillRunning = new AtomicBoolean(false);

    static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Scheduled(cron = "${dms.cron}")
    @LogExecutionTime
    protected void scheduledTask() throws S3Exception {
        if (!stillRunning.get()) {
            stillRunning.set(true);
            try {
                this.csvToDb.processFiles();
                this.dbToEakte.start();
            } finally {
                stillRunning.set(false);
            }
        } else {
            log.info("Nothing to do, scheduled task still running");
        }
    }

}
