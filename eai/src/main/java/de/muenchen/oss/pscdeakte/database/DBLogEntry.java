package de.muenchen.oss.pscdeakte.database;

public record DBLogEntry(String level, String message, String exception, String gpId) {
}
