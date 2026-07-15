package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class MetadataException extends Exception {
    public MetadataException(final String message) {
        super(message);
    }

    public MetadataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
