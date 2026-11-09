package de.muenchen.oss.pscdeakte.csv;

import lombok.Getter;

import java.io.Serial;

@Getter
public class DBException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String gpId;

    public DBException(final String gpId, final Throwable cause) {
        super(cause);
        this.gpId = gpId;
    }

}
