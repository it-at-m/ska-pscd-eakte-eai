package de.muenchen.oss.pscdeakte.csv;

import lombok.Getter;

@Getter
public class CSVException extends RuntimeException {

    public CSVException(final String gpId, final Throwable cause) {
        super(cause);
        this.gpId = gpId;
    }

    final String gpId;

}
