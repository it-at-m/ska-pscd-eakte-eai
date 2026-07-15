package de.muenchen.oss.ska_pscd_eakte_eai.swimdms.adapter.out.dms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "pscd.dms")
@Data
@Validated
@ToString(exclude = "password")
public class DmsProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
