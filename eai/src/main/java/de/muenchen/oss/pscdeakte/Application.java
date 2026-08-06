package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.io.IOException;

@ConfigurationPropertiesScan
@SpringBootApplication
@RequiredArgsConstructor
@SuppressWarnings("PMD.UseUtilityClass")
public class Application {
    private final ApplicationContext context;
    private final CsvToDb csvToDb;
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

//    @EventListener(ApplicationReadyEvent.class)
    public void csvToDb() throws S3Exception, IOException {
        this.csvToDb.saveBucketToDb("todo"); //TODO filename der csv im Bucket
        SpringApplication.exit(context);
        // fail with "Socket accept failed" is ok
    }


}

