package de.muenchen.oss.pscdeakte.csv;

import de.muenchen.oss.pscdeakte.configuration.annotation.LogExecutionTime;
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
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvToDbService extends DBLogger {

    public static final String TEST_GPID = "1000000000";
    @Getter
    private final S3OutPort s3;
    private final S3Properties props;
    private final PscdImportRepository pir;

    /* package */ enum HEADERS {
        GP_ID,
        NAME,
        VORNAME,
        GEB_DAT,
        ZENTRALAKTKENNUNG
    }

    public void processFiles() throws S3Exception {
        log.info("Processing CSV files");
        getFilesAsListResult().files().forEach(file -> saveFileToDb(file.path()));
    }

    protected ListResult getFilesAsListResult() throws S3Exception {
        log.info("#getFilesAsListResult()");
        final ListResult foundFiles = s3.getFilesAsListResult(props.getBucket(), props.getPrefix(), true);
        log.info("{} files found", foundFiles.files().size());
        return foundFiles;
    }

    @LogExecutionTime
    protected void saveFileToDb(final String filename) {
        log.info("reading file {}", filename);
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(props.getDelimiter())
                .setEscape(props.getEscape())
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(props.getSkipHeader())
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
            log.error("reading/moving file {} failed: {}", filename, e.getMessage());
            log("ERROR", "reading/moving file " + filename + " failed", e.getMessage(), null);
        } catch (DBException e) {
            log.error("Exception von DB bei file {}, GP-Id {}: {}", filename, e.getGpId(), e.getMessage());
            log("ERROR", "Exception von DB bei file " + filename + ", GP-Id " + e.getGpId(), e.getMessage(), e.getGpId());
        }

    }

    protected void processCSVRecord(final CSVRecord csvRecord) {
        final PscdImport fromCsv = mapData(csvRecord);
        if (TEST_GPID.equals(fromCsv.getGeschaeftspartnerId())) {
            // Test GP-Id ueberspringen
            return;
        }
        try {
            final Optional<PscdImport> fromDb = pir.findByGeschaeftspartnerId(fromCsv.getGeschaeftspartnerId());
            if (fromDb.isEmpty()) {
                pir.save(fromCsv);
                return;
            }
            final DuplicateOrUpdate dou = new DuplicateOrUpdate(fromDb.get(), fromCsv);
            if (dou.isUpdate()) {
                pir.save(dou.createUpdatedPscdImport());
            }
        } catch (Exception e) {
            throw new DBException(fromCsv.getGeschaeftspartnerId(), e);
        }
    }

    protected PscdImport mapData(final CSVRecord csvRecord) {
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
