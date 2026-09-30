package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.io.FileReader;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    void csvSetQuoteTest() throws IOException {
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(";")
                .setQuote('µ')
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(false)
                .get();
        final Iterable<CSVRecord> records;
        records = csvFormat.parse(new FileReader("testdata/s3/BP_Export_invalid_character.csv"));
        Assertions.assertEquals("\"innen\" aussen", mapData(records.iterator().next()).getName());
    }

    @Test
    void csvQuotedTest() throws IOException {
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(";")
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(false)
                .get();
        final Iterable<CSVRecord> records;
        records = csvFormat.parse(new FileReader("testdata/s3/BP_Export_invalid_character_quoted.csv"));
        Assertions.assertEquals("\"innen\" aussen", mapData(records.iterator().next()).getName());
    }

    @Test
    void csvQuoteEscapedTest() throws IOException {
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(";")
                .setEscape('\\')
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(false)
                .get();
        final Iterable<CSVRecord> records;
        records = csvFormat.parse(new FileReader("testdata/s3/BP_Export_invalid_character_escaped.csv"));
        Assertions.assertEquals("\"innen\" aussen", mapData(records.iterator().next()).getName());
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
