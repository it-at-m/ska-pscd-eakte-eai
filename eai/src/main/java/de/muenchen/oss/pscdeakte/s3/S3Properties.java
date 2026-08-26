package de.muenchen.oss.pscdeakte.s3;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "s3csv")
@Data
@Validated
public class S3Properties {
    @NotBlank private String bucket;
    @NotBlank private String prefix;
    private String delimiter = ";";
    private boolean skipHeader = false;
}
