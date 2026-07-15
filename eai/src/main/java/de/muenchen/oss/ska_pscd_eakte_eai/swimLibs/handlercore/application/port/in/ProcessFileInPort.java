package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.application.port.in;

import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.SingleFileEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessFileInPort {
    /**
     * Start processing the given file.
     *
     * @param event The event for the file to process.
     * @throws PresignedUrlException Is thrown when presign url can't be parsed or isn't valid.
     * @throws UnknownUseCaseException Is thrown when use case name isn't known.
     */
    void processEvent(@Valid @NotNull SingleFileEvent event)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException;

    /**
     * Start processing the given files.
     *
     * @param event The event for the files to process.
     * @throws PresignedUrlException Is thrown when presign url can't be parsed or isn't valid.
     * @throws UnknownUseCaseException Is thrown when use case name isn't known.
     */
    void processEvent(@Valid @NotNull MultiFileEvent event)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException;
}
