package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Slf4j
class CsvTest {

    enum HEADERS {
        GP_ID,
        NAME,
        VORNAME,
        GEB_DAT,
        ZENTRALAKTKENNUNG
    }

    @Test
    void readCsvTest() throws IOException {
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(";")
                .setQuote('µ')
//                .setEscape('\\')
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(false)
                .get();
        final Iterable<CSVRecord> records;
        records = csvFormat.parse(new FileReader(new File("testdata/s3/BP_Export_invalid_character.csv")));
        Assertions.assertEquals("\"facts\" Veranstaltungsmanagement GmbH, Austria, Zweigniederlassung München", mapData(records.iterator().next()).getName());
//        Assertions.assertEquals("\"facts2\" Veranstaltungsmanagement", mapData(records.iterator().next()).getName());
    }

    @LogExecutionTime
    public PscdImport mapData(final CSVRecord csvRecord) {
        log.debug("mapping GP {}", csvRecord.get(HEADERS.GP_ID));
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId(csvRecord.get(HEADERS.GP_ID));
        data.setName(csvRecord.get(HEADERS.NAME));
        data.setVorname(csvRecord.get(HEADERS.VORNAME));
        data.setGeburtsdatum(csvRecord.get(HEADERS.GEB_DAT));
        data.setZentralakt(csvRecord.get(HEADERS.ZENTRALAKTKENNUNG));
        data.setStatus(DatensatzStatus.NEW);
        return data;
    }

}
