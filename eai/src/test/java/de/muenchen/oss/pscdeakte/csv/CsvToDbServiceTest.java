package de.muenchen.oss.pscdeakte.csv;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.s3.S3Properties;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileMetadata;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CsvToDbServiceTest {

    @Mock
    private S3OutPort s3OutPort;

    private CsvToDbService csvToDbService;

    private S3Properties s3Properties;

    @BeforeEach
    public void beforeEach() {
        s3Properties = new S3Properties();
        s3Properties.setBucket("defaultBucket");
        s3Properties.setPrefix("prefix");
        s3Properties.setDelimiter(";");
        s3Properties.setSkipHeader(false);
        csvToDbService = new CsvToDbService(s3OutPort, s3Properties, null, null);
        Mockito.reset(s3OutPort);
    }

    @Test
    void getFilesWithPrefix() throws S3Exception {
        final var fileMetadata1 = new FileMetadata(
                "dummy.csv",
                999L,
                "etag",
                Instant.now());

        final ListResult foundFiles = new ListResult(List.of(fileMetadata1), List.of("prefix"), false, "startAfter");
        Mockito.when(s3OutPort.getFilesWithPrefix(s3Properties.getBucket(), s3Properties.getPrefix(), true)).thenReturn(foundFiles);

        csvToDbService.getFilesWithPrefix();

        Mockito
                .verify(s3OutPort, Mockito.times(1))
                .getFilesWithPrefix(s3Properties.getBucket(), s3Properties.getPrefix(), true);
    }

    @Test
    void mapData() throws IOException {
        final PscdImport expected = new PscdImport();
        expected.setGeschaeftspartnerId("2000000000");
        expected.setName("s3testname");
        expected.setVorname("s3testvorname");
        expected.setGeburtsdatum("01.02.2012");
        expected.setZentralakt("1234");
        expected.setStatus(DatensatzStatus.NEW);

        FileInputStream fileInputStream = new FileInputStream("src/test/resources/BP_Export_Test.csv");
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(s3Properties.getDelimiter())
                .setHeader(CsvToDbService.HEADERS.class)
                .setSkipHeaderRecord(s3Properties.isSkipHeader())
                .get();

        final CSVParser csvRecords = csvFormat.parse(new InputStreamReader(fileInputStream, StandardCharsets.ISO_8859_1));

        final List<CSVRecord> records = csvRecords.getRecords();
        Assertions.assertThat(records).isNotNull();
        Assertions.assertThat(records.size()).isEqualTo(1);

        final CSVRecord csvRecord = records.getFirst();
        final PscdImport result = csvToDbService.mapData(csvRecord);

        Assertions.assertThat(result).usingRecursiveComparison().isEqualTo(expected);

    }

}
