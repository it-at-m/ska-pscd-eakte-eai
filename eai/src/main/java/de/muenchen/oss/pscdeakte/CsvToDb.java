package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.DmsProperties;
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

    @Getter
    private final S3OutPort s3;
    private final S3Properties props;
    private final DmsProperties dmsProps;
    private final PscdImportRepository pir;
    private final DBLogger logDb;

    enum HEADERS {
        GP_ID,
        NAME,
        VORNAME,
        GEB_DAT,
        ZENTRALAKTKENNUNG
    }

    public void saveFilesToDb(String prefix) throws S3Exception {
        ListResult list = this.getFilesWithPrefix(prefix);
        log.info("{} files found", list.files().size());
        list.files().forEach(file -> saveFileToDb(file.path()));
        //        TODO verarbeitete csv loeschen?
    }

    public ListResult getFilesWithPrefix(final String prefix) throws S3Exception {
        return s3.getFilesWithPrefix(props.getBucket(), prefix, true);
    }

    public void saveFileToDb(final String filename) {
        log.info("reading file {}", filename);
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(props.getDelimiter())
                .setHeader(HEADERS.class)
                .setSkipHeaderRecord(props.isSkipHeader())
                .get();
        final Iterable<CSVRecord> records;
        try {
            records = csvFormat.parse(new InputStreamReader(s3.getFileContent(new FileReference(props.getBucket(), filename)), StandardCharsets.ISO_8859_1));
            records.forEach(csvRecord -> pir.save(this.mapData(csvRecord)));
            //            TODO Datenbankfehler abfangen
        } catch (IOException | S3Exception e) {
            logDb.log("ERROR", "reading file " + filename + "failed", e.getMessage());
        }

    }

    private PscdImport mapData(final CSVRecord csvRecord) {
        log.info("mapping GP {}", csvRecord.get(HEADERS.GP_ID));
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId(csvRecord.get(HEADERS.GP_ID));
        data.setName(csvRecord.get(HEADERS.NAME));
        data.setVorname(csvRecord.get(HEADERS.VORNAME));
        data.setGeburtsdatum(csvRecord.get(HEADERS.GEB_DAT));
        data.setZentralakt(csvRecord.get(HEADERS.ZENTRALAKTKENNUNG));
        if (dmsProps.isInitialbefuellung()) {
            data.setStatus(DatensatzStatus.NEW);
        } else {
            data.setStatus(pir.countByGeschaeftspartnerId(data.getGeschaeftspartnerId()) == 0 ? DatensatzStatus.NEW : DatensatzStatus.DUPLICATE);
        }
        return data;
    }

}
