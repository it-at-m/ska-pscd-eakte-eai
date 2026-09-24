package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.s3.S3Properties;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvToDb {

    public static final String TEST_GPID = "1000000000";
    @Getter
    private final S3OutPort s3;
    private final S3Properties props;
    private final PscdImportRepository pir;
    private final DBLogger logDb;

    /* package */ enum HEADERS {
        GP_ID,
        NAME,
        VORNAME,
        GEB_DAT,
        ZENTRALAKTKENNUNG
    }

    public void processFiles() throws S3Exception {
        log.info("Processing CSV files");
        final ListResult list = s3.getFilesWithPrefix(props.getBucket(), props.getPrefix(), true);
        log.info("{} files found", list.files().size());
        list.files().forEach(file -> saveFileToDb(file.path()));
    }

    @LogExecutionTime
    public void saveFileToDb(final String filename) {
        log.info("reading file {}", filename);
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(props.getDelimiter())
                .setEscape(props.getEscape())
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(props.isSkipHeader())
                .get();
        final Iterable<CSVRecord> records;
        try {
            final FileReference fileReference = new FileReference(props.getBucket(), filename);
            records = csvFormat.parse(new InputStreamReader(s3.getFileContent(fileReference), StandardCharsets.ISO_8859_1));
            records.forEach(this::processCSVRecord);
            final String movedFile = "." + filename;
            s3.copyFile(fileReference, new FileReference(props.getBackupBucket(), movedFile));
            s3.deleteFile(fileReference);
            log.info("moved file {}/{} to {}/{}", props.getBucket(), filename, props.getBackupBucket(), movedFile);
        } catch (IOException | S3Exception e) {
            //            TODO Datenbankfehler abfangen
            logDb.log("ERROR", "reading/moving file " + filename + " failed", e.getMessage());
        }

    }

    @LogExecutionTime
    public void processCSVRecord(final CSVRecord csvRecord) {
        final PscdImport fromCsv = mapData(csvRecord);
        if (TEST_GPID.equals(fromCsv.getGeschaeftspartnerId())) {
            // Test GP-Id ueberspringen
            return;
        }
        final PscdImport fromDb = pir.findByGeschaeftspartnerId(fromCsv.getGeschaeftspartnerId());
        if (fromDb == null) {
            pir.save(fromCsv);
            return;
        }
        final DuplicateOrUpdate dou = new DuplicateOrUpdate(fromDb, fromCsv);
        if (dou.isUpdate()) {
            pir.save(dou.createUpdatedPscdImport());
        }
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
