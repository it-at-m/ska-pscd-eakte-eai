package de.muenchen.oss.ska_pscd_eakte_eai.swimdms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class DmsException extends RuntimeException {
    public DmsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DmsException(final String message) {
        super(message);
    }
}
