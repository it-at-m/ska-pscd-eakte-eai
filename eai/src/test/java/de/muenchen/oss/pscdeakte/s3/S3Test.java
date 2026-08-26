package de.muenchen.oss.pscdeakte.s3;

import de.muenchen.oss.pscdeakte.CsvToDb;
import de.muenchen.oss.pscdeakte.TestConstants;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.refarch.integration.s3.adapter.out.s3.S3Mapper;
import de.muenchen.oss.refarch.integration.s3.adapter.out.s3.S3OutAdapter;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Slf4j
@Disabled
class S3Test {

    private S3OutPort s3OutPort;
    private final S3Properties props;
    private final CsvToDb csvToDb;
    private final PscdImportRepository repo;

    @Autowired
    S3Test(S3Properties props, CsvToDb csvToDb, PscdImportRepository repo) {
        this.props = props;
        this.csvToDb = csvToDb;
        this.repo = repo;
    }

    private static final String ACCESS_KEY = "minio";
    private static final String SECRET_KEY = "Test1234";
    private static final String BUCKET = "int-eheaik-importrueckstandsakt";

    @Container
    private static final GenericContainer<?> MINIO = new GenericContainer<>("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data", "--console-address", ":9001")
            .withExposedPorts(9000, 9001);

    @BeforeAll
    @SuppressWarnings("PMD.CloseResource")
    void setUp() {
        final String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        final Region region = Region.US_EAST_1;

        final S3Configuration s3cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        final StaticCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

        final S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg)
                .build();
        final S3Presigner s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg)
                .build();

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
            //            ignored
        }

        final S3Mapper mapper = new S3Mapper();
        this.s3OutPort = new S3OutAdapter(mapper, s3Client, s3Presigner);
    }

    @Test
    void simpleS3Test() throws S3Exception, IOException {

        final String testfile = "testdata/s3/s3testfile";

        final FileReference fileReference = new FileReference(props.getBucket(), "s3testfile");
        s3OutPort.saveFile(fileReference, new File(testfile));
        Assertions.assertDoesNotThrow(() -> s3OutPort.saveFile(fileReference, new File(testfile)));

        final ListResult result = s3OutPort.getFilesWithPrefix(props.getBucket(), "s", true);
        final String path = result.files().getFirst().path();
        Assertions.assertEquals("s3testfile", path);
        final FileReference fileReference1 = new FileReference(props.getBucket(), path);
        Assertions.assertEquals("content", new BufferedReader(new InputStreamReader(s3OutPort.getFileContent(fileReference1))).readLine());

        Assertions.assertDoesNotThrow(() -> s3OutPort.deleteFile(fileReference1));

    }

    @Test
    void integratedTest() {
        final String filename = "BP_Export_Test.csv";
        FileReference fileReference = new FileReference(props.getBucket(), filename);
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().saveFile(fileReference, new File("testdata/s3/" + filename)));
        Assertions.assertDoesNotThrow(() -> csvToDb.saveFilesToDb(props.getPrefix()));
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().deleteFile(fileReference));
        final String gpId = "2000000000";
        final List<PscdImport> list = repo.findByGeschaeftspartnerId(gpId);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(gpId, list.getFirst().getGeschaeftspartnerId());
        Assertions.assertEquals("01.02.2012", list.getFirst().getGeburtsdatum());
        Assertions.assertDoesNotThrow(() -> repo.delete(list.getFirst()));
    }
}
