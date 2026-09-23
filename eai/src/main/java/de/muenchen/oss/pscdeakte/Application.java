package de.muenchen.oss.pscdeakte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class Application {
    /* package */ static void main(final String... args) {
        SpringApplication.run(Application.class, args);
    }
}
