package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class UnknownUseCaseException extends Exception {
    public UnknownUseCaseException(final String message) {
        super(message);
    }
}
