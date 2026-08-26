package de.muenchen.oss.pscdeakte.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DatensatzStatus {
    NEW("neu"),
    STARTED("gestartet"),
    APENTRY_EXISTS("Betreffseinheit angelegt/vorhanden"),
    FILE_CREATED("Akte angelegt"),
    BESTANDSAKT_CREATED("\"Bestandsakten\" angelegt"),
    DONE("erfolgreich verarbeitet"),
    DUPLICATE("Duplikat"),
    ARCHIVE("personenbezogene Daten entfernt"),
    ERROR("Fehler: manuelles Eingreifen erforderlich");

    private final String value;

}
