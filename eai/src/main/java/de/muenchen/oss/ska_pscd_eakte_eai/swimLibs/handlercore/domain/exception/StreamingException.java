package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class StreamingException extends RuntimeException {
    public StreamingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
