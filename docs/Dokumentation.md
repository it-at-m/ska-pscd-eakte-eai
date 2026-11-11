# Dokumentation der EAI pscd Import in die eAkte
## Konfiguration
### Allgemeine Properties

| Property                | Erklärung                                                                                         | Default |
|-------------------------|---------------------------------------------------------------------------------------------------|---------|
| SPRING_FLYWAY_SCHEMAS   | Schema in der Datenbank                                                                           | -       |
| DMS_XANWENDUNG          | Name des ausführenden Fachverfahren                                                               | -       |
| DMS_AKTENPLANNUMMER     | Geschäftszeichen (aka Aktenplankennzeichen) des Übergeordneten Aktenplaneintrages ("Einzelakten") | -       |
| DMS_JOBPOSITION         | Rolle des Organisationsuser in der eAkte                                                          | -       |
| EAKTE_CONNECTIONTIMEOUT | Timeout für die Verbindung zur eAkte API                                                          | 30sec   |
| EAKTE_RESPONSETIMEOUT   | Timeout für die Antwort der eAkte API                                                             | 120sec  |
| S3CSV_DELIMITER         | Trennzeichen der CSV                                                                              | ';'     |
| S3CSV_SKIPHEADER        | Gibt an ob eine Headerzeile in der CSV übersprungen werden muss                                   | false   |
| S2CSV_ESCAPE            | Escape-Character in der CSV                                                                       | '\\'    |


### Umgebungsspezifische ConfigMaps

| Property              | Erklärung                                                         | Default |
|-----------------------|-------------------------------------------------------------------|---------|
| SPRING_DATASOURCE_URL | URl zum Datenbankserver                                           | -       |
| EAKTE_BASEURL         | URL zur DMS-EAI                                                   | -       |
| DMS_COOEINZELAKTE     | COO-Adresse der Einzelakten                                       | -       |
| DMS_COOKMAKTE         | COO-Adresse der Vorlage Definition für Verfahren "KM Akte"        | -       |
| DMS_JOBOE             | COO-Adresse der Orgeinheit des Organisationsuser                  | -       |
| DMS_INITIALBEFUELLUNG | Schalter, um festzulegen, ob es sich um die Erstbefüllung handelt | false   |
| DMS_CRON              | Cronjob, aufgrund dessen die Verarbeitung gestartet wird          | -       |
| REFARCH_S3_URL        | URL zum S3-Server                                                 | -       |
| S3CSV_BUCKET          | Name des Buckets, indem die einzulesenden Dateien liegen          | -       |
| S3CSV_BACKUPBUCKET    | Name des Buckets, indem die eingelesenen Dateien abgelegt werden  | -       |
| S3CSV_PREFIX          | Prefix der einzulesenden Dateien                                  | -       |

### Secrets
Es werden zum Starten in der CAP folgende Secrets benötigt

#### pscd-import-s3 
| Property             | Erklärung            | 
|----------------------|----------------------|
| REFARCH_S3_ACCESSKEY | Accesskey für den S3 |
| REFARCH_S3_SECRETKEY | Secretkey für den S3 |

#### pscd-import-dms
| Property       | Erklärung                               | 
|----------------|-----------------------------------------|
| EAKTE_USERNAME | Nutzername für die eAkte API            |
| EAKTE_PASSWORD | Passwort für die eAkte API              |
| DMS_USERLOGIN  | Userlogin für den Zugriff auf die eAkte |

#### pscd-import-db
| Property                   | Erklärung                                    | 
|----------------------------|----------------------------------------------|
| SPRING_DATASOURCE_USERNAME | Nutzername für den Zugriff auf die Datenbank |
| SPRING_DATASOURCE_PASSWORD | Passwort für den Zugriff auf die Datenbank   |

### Datenbank

Unter dem Schema 'pscdeakte' sind drei Tabellen angelegt:
- 'flyway_schema_history' wird von Flyway zur Aktualisierung der beiden anderen Tabellen verwaltet
- 'logs' dient dem Logging.
- 'pscd_import' enthält die Daten des Pscd-Imports 

#### Felder in pscd_import

##### geschaeftspartner_id
eindeutige ID der Datenbank; wird aus csv-Datei gefüllt
##### name
wird aus csv-Datei gefüllt
##### vorname
wird aus csv-Datei gefüllt
##### geburtsdatum
wird aus csv-Datei gefüllt
##### zentralakt
wird aus csv-Datei gefüllt
##### betreffseinheit
coo-Adresse der Betreffseinheit
##### akte
coo-Adresse der Einzelakte
##### bestandsakt
coo-Adresse des Vorgangs "Bestandsakten"
##### av
coo-Adresse des Vorgangs "AVs, Titel, Haftbefehle"
##### Status
Folgende Werte können im Feld 'status' auftreten:
'NEW'
'STARTED'
'APENTRY_EXISTS'
'FILE_CREATED'
'BESTANDSAKT_CREATED'
'DONE'
'UPDATE'

Bedeutung von 'NEW':
Der Datensatz wurde aus der CSV-Datei in die Datenbank eingelesen aber noch nicht vom DmsService erfasst.
Die Datenbankfelder für die coo-Adressen sind alle 'NULL'

Bedeutung von 'STARTED':
Der Datensatz wurde vom DmsService erfasst. Es wurde noch kein Arbeitsschritt abgeschlossen.
Die Datenbankfelder für die coo-Adressen sind alle 'NULL'.

Bedeutung von 'APENTRY_EXISTS':
Die dazugehörige Betreffseinheit ist angelegt bzw. existiert bereits in der eAkte.
Im Datenbankfeld 'betreffseinheit' steht deren coo-Adresse. Andere Datenbankfelder für coo-Adressen sind 'NULL'.

Bedeutung von 'FILE_CREATED':
Die Einzelakte ist angelegt.
Im Datenbankfeld 'akte' steht deren coo-Adresse. Die Felder 'bestandsakt' und 'av' sind noch 'NULL'.

Bedeutung von 'BESTANDSAKT_CREATED':
Der Vorgang "Bestandsakt" ist angelegt.
Die coo-Adresse des Vorgangs steht im Feld 'bestandsakt'. Feld 'av' ist noch 'NULL'.

Bedeutung von 'DONE':
Der Vorgang "AVs, Titel, Haftbefehle" ist angelegt.
Das Feld 'av' enthält die coo-Adresse des Vorgangs.
Damit ist die Verarbeitung des Datensatzes abgeschlossen.

Bedeutung von 'UPDATE':
Beim Lesen der csv-Datei wurde ein Datensatz mit derselben Geschäftspartner-Id in der Datenbank gefunden.
Zusätzlich unterscheidet sich mindestens ein Wert in den Feldern 'name', 'vorname', 'geburtsdatum' oder 'zentralakt'.
Der DmsService wird beim darauffolgenden Lauf die Daten in der bereits bestehende Einzelakte aktualisieren.
Alle vier Felder mit coo-Adressen sind gefüllt. 

##### created_time
Wann der Datensatz angelegt wurde. Wird automatisch gesetzt.
##### updated_time
Zeitpunkt der letzten Änderung. Wird automatisch gesetzt.