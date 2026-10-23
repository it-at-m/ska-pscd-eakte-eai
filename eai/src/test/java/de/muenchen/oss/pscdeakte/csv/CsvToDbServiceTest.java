package de.muenchen.oss.pscdeakte.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.s3.S3Properties;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileMetadata;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvToDbServiceTest {

    @Mock
    private S3OutPort s3OutPort;

    @Mock
    private PscdImportRepository pscdImportRepository;

    @Mock
    private DBLogger dbLogger;

    private CsvToDbService csvToDbService;

    private S3Properties s3Properties;

    @BeforeEach
    void beforeEach() {
        s3Properties = new S3Properties();
        s3Properties.setBucket("defaultBucket");
        s3Properties.setBackupBucket("backupBucket");
        s3Properties.setPrefix("prefix");
        s3Properties.setDelimiter(";");
        s3Properties.setSkipHeader(false);
        csvToDbService = new CsvToDbService(s3OutPort, s3Properties, pscdImportRepository, dbLogger);
    }

    @Test
    void getFilesAsListResultReturnsS3Result() throws S3Exception {
        final ListResult foundFiles = new ListResult(List.of(fileMetadata("dummy.csv")), List.of("prefix"), false);
        when(s3OutPort.getFilesAsListResult(s3Properties.getBucket(), s3Properties.getPrefix(), true)).thenReturn(foundFiles);

        assertSame(foundFiles, csvToDbService.getFilesAsListResult());
    }

    @Test
    void processFilesImportsMovesAndDeletesFile() throws S3Exception {
        final ListResult foundFiles = new ListResult(List.of(fileMetadata("input.csv")), List.of(), false);
        when(s3OutPort.getFilesAsListResult(s3Properties.getBucket(), s3Properties.getPrefix(), true)).thenReturn(foundFiles);
        when(s3OutPort.getFileContent(any(FileReference.class)))
                .thenReturn(new ByteArrayInputStream("2000000000;Name;Vorname;01.02.2020;zentralakt".getBytes(StandardCharsets.ISO_8859_1)));
        when(pscdImportRepository.findByGeschaeftspartnerId("2000000000")).thenReturn(Optional.empty());

        csvToDbService.processFiles();

        final ArgumentCaptor<PscdImport> dataCaptor = ArgumentCaptor.forClass(PscdImport.class);
        verify(pscdImportRepository).save(dataCaptor.capture());
        assertEquals("2000000000", dataCaptor.getValue().getGeschaeftspartnerId());
        assertEquals(DatensatzStatus.NEW, dataCaptor.getValue().getStatus());
        verify(s3OutPort).copyFile(new FileReference("defaultBucket", "input.csv"), new FileReference("backupBucket", ".input.csv"));
        verify(s3OutPort).deleteFile(new FileReference("defaultBucket", "input.csv"));
    }

    @Test
    void saveFileToDbLogsErrorWhenS3ReadFails() throws S3Exception {
        final S3Exception exception = Mockito.mock(S3Exception.class);
        when(exception.getMessage()).thenReturn("S3 unavailable");
        when(s3OutPort.getFileContent(any(FileReference.class))).thenThrow(exception);

        csvToDbService.saveFileToDb("input.csv");

        verify(dbLogger).log("ERROR", "reading/moving file input.csv failed", "S3 unavailable");
    }

    @Test
    void processCSVRecordSkipsTestGpId() {
        csvToDbService.processCSVRecord(record(CsvToDbService.TEST_GPID, "Name", "Vorname", "01.02.2020", "zentralakt"));

        verifyNoInteractions(pscdImportRepository);
    }

    @Test
    void processCSVRecordSavesNewRecord() {
        final CSVRecord record = record("2000000000", "Name", "Vorname", "01.02.2020", "zentralakt");
        when(pscdImportRepository.findByGeschaeftspartnerId("2000000000")).thenReturn(Optional.empty());

        csvToDbService.processCSVRecord(record);

        verify(pscdImportRepository).save(any(PscdImport.class));
    }

    @Test
    void processCSVRecordDoesNotSaveDuplicate() {
        final CSVRecord record = record("2000000000", "Name", "Vorname", "01.02.2020", "zentralakt");
        when(pscdImportRepository.findByGeschaeftspartnerId("2000000000"))
                .thenReturn(Optional.of(importData("2000000000", "Name", "Vorname", "01.02.2020", "zentralakt")));

        csvToDbService.processCSVRecord(record);

        verify(pscdImportRepository, never()).save(any(PscdImport.class));
    }

    @Test
    void processCSVRecordSavesUpdateForChangedRecord() {
        final CSVRecord record = record("2000000000", "New name", "Vorname", "01.02.2020", "zentralakt");
        final PscdImport existingImport = importData("2000000000", "Name", "Vorname", "01.02.2020", "zentralakt");
        existingImport.setAkte("akte");
        when(pscdImportRepository.findByGeschaeftspartnerId("2000000000")).thenReturn(Optional.of(existingImport));

        csvToDbService.processCSVRecord(record);

        final ArgumentCaptor<PscdImport> dataCaptor = ArgumentCaptor.forClass(PscdImport.class);
        verify(pscdImportRepository).save(dataCaptor.capture());
        assertEquals("New name", dataCaptor.getValue().getName());
        assertEquals("akte", dataCaptor.getValue().getAkte());
        assertEquals(DatensatzStatus.UPDATE, dataCaptor.getValue().getStatus());
    }

    @Test
    void mapDataMapsAllColumns() {
        final PscdImport result = csvToDbService.mapData(record("2000000000", "Name", "Vorname", "01.02.2020", "zentralakt"));

        assertEquals("2000000000", result.getGeschaeftspartnerId());
        assertEquals("Name", result.getName());
        assertEquals("Vorname", result.getVorname());
        assertEquals("01.02.2020", result.getGeburtsdatum());
        assertEquals("zentralakt", result.getZentralakt());
        assertEquals(DatensatzStatus.NEW, result.getStatus());
    }

    private static FileMetadata fileMetadata(final String path) {
        return new FileMetadata(path, 999L, "etag", Instant.now());
    }

    private static CSVRecord record(final String gpId, final String name, final String vorname, final String geburtsdatum, final String zentralakt) {
        final CSVRecord record = Mockito.mock(CSVRecord.class);
        when(record.get(CsvToDbService.HEADERS.GP_ID)).thenReturn(gpId);
        when(record.get(CsvToDbService.HEADERS.NAME)).thenReturn(name);
        when(record.get(CsvToDbService.HEADERS.VORNAME)).thenReturn(vorname);
        when(record.get(CsvToDbService.HEADERS.GEB_DAT)).thenReturn(geburtsdatum);
        when(record.get(CsvToDbService.HEADERS.ZENTRALAKTKENNUNG)).thenReturn(zentralakt);
        return record;
    }

    private static PscdImport importData(final String gpId, final String name, final String vorname, final String geburtsdatum, final String zentralakt) {
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId(gpId);
        data.setName(name);
        data.setVorname(vorname);
        data.setGeburtsdatum(geburtsdatum);
        data.setZentralakt(zentralakt);
        return data;
    }

}
