package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.adapter.in;

import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.FileEvent;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.SingleFileEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingInAdapter {
    private final ProcessFileInPort processFileInPort;

    /**
     * Consumer for dispatch events sent via Kafka from the dispatch-service.
     *
     * @return The consumer.
     */
    @Bean
    public Consumer<Message<FileEvent>> event() {
        return message -> {
            final FileEvent fileEvent = message.getPayload();
            try {
                if (fileEvent instanceof SingleFileEvent single) {
                    processFileInPort.processEvent(single);
                } else if (fileEvent instanceof MultiFileEvent multi) {
                    processFileInPort.processEvent(multi);
                } else {
                    throw new IllegalArgumentException("FileEvent of type '%s' isn't supported".formatted(fileEvent.getClass().getName()));
                }
            } catch (final PresignedUrlException | UnknownUseCaseException | MetadataException e) {
                log.warn("Error while processing event in use case {}: {}", fileEvent.useCase(), fileEvent, e);
                throw new FileProcessingException(e);
            }
        };
    }
}
