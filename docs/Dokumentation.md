# Dokumentation der EAI pscd Import in die eAkte
## Konfiguration
### configMaps

'dms.cron':     Cronjob, aufgrund dessen die Verarbeitung gestartet wird. 
'dms.timeout':  Timeout zur REST-API an die eAkte in Sekunden.

### Datenbank

Unter dem Schema 'pscdeakte' sind drei Tabellen angelegt:
- 'flyway_schema_history' wird von Flyway zur Aktualisierung der beiden anderen Tabellen verwaltet
- 'logs' dient dem Logging.
- 'pscd_import' enthält die Daten des Pscd-Imports 

#### Felder in pscd_import

##### geschaeftspartner_id
eindeutige ID der Datenbank, wird aus csv-Datei gefüllt
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

##### created_at
Wann der Datensatz angelegt wurde. Wird von der Datenbank verwaltet.
##### updated_at
Zeitpunkt der letzten Änderung. Wird von der Datenbank verwaltet