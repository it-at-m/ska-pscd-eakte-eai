package de.muenchen.oss.pscdeakte.csv;

import lombok.Getter;

@Getter
public class DBException extends RuntimeException {

    public DBException(final String gpId, final Throwable cause) {
        super(cause);
        this.gpId = gpId;
    }

    final String gpId;

}
