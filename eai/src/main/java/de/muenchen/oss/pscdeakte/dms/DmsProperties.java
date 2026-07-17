package de.muenchen.oss.pscdeakte.dms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "dms")
@Data
@Validated
@ToString(exclude = "password")
public class DmsProperties {
    @NotBlank
    private String xAnwendung;
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String einzelakte;
    @NotBlank
    private String jobposition;
    @NotBlank
    private String joboe;
    @NotBlank
    private String userlogin;
}
