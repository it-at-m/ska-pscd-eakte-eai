package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.s3.S3Properties;
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
    private final S3Properties props;
    private final DbToEakte dbToEakte;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    // Review Laut SysSpec soll die EAI auf ein neues File lauschen und dann direkt loslegen
    public void processFiles() throws S3Exception {
        // REVIEW: Ich hatte für DAVe einen Laufzeitlogger gebaut, der ausgibt wie lange etwas gedauert hat. Evtl ist das was
        // auch für hier. Das war nur eine Annotation an die Methode und fertig
        log.info("Loading CSV files");
        this.csvToDb.saveFilesToDb(props.getPrefix());
        log.info("Reading Database");
        this.dbToEakte.start();
        log.info("shutdown");
    }

}
