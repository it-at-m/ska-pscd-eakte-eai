package de.muenchen.oss.pscdeakte.database;

import lombok.Getter;

@Getter
public enum DatensatzStatus {
    NEW("neu"),
    STARTED("gestartet"),
    DUPLICATE("duplikat"),
    APENTRY_EXISTS("Betreffseinheit angelegt/vorhanden"),
    FILE_CREATED("Akte angelegt"),
    BESTANDSAKT_CREATED("\"Bestandsakten\" angelegt"),
    AV_CREATED("\"AVs, Titel, Haftbefehle\" angelegt"),
    DONE("erfolgreich verarbeitet"),
    ARCHIVE("personenbezogene Daten entfernt"),
    ERROR("Fehler: manuelles Eingreifen erforderlich");

    private final String value;

    DatensatzStatus(String value) {
        this.value = value;
    }
}
