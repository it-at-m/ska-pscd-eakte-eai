package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.application.port.out;

import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.PresignedUrlException;
import jakarta.validation.constraints.NotBlank;
import java.io.InputStream;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileSystemOutPort {
    /**
     * Get file via presigned url.
     *
     * @param presignedUrl The presigned url for the file.
     * @return The file.
     */
    InputStream getPresignedUrlFile(@NotBlank String presignedUrl) throws PresignedUrlException;
}
