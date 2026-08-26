package de.muenchen.oss.pscdeakte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import de.muenchen.oss.refarch.integration.s3.adapter.out.s3.S3Mapper;
import de.muenchen.oss.refarch.integration.s3.adapter.out.s3.S3OutAdapter;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
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

@SpringBootTest(classes = { Application.class })
@CamelSpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Testcontainers
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class DatabaseTest {
    private static final String ACCESS_KEY = "minio";
    private static final String SECRET_KEY = "Test1234";
    private static final String BUCKET = "test-bucket";

    @Container
    private static final GenericContainer<?> MINIO = new GenericContainer<>("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data", "--console-address", ":9001")
            .withExposedPorts(9000, 9001);

    static S3OutPort s3OutPort;


    private final PscdImportRepository pir;

    @Autowired
    public DatabaseTest(PscdImportRepository pir) {
        this.pir = pir;
    }

    @BeforeAll
    @SuppressWarnings("PMD.CloseResource")
    static void setUp() {
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
        }

        final S3Mapper mapper = new S3Mapper();
        DatabaseTest.s3OutPort = new S3OutAdapter(mapper, s3Client, s3Presigner);
    }

    @Test
    void test_pscdimport_crud() {

        PscdImport pscdImport = new PscdImport();
        pscdImport.setName("name");
        pscdImport.setVorname("vorname");
        PscdImport saved = pir.save(pscdImport);
        assertNotNull(saved.getLastUpdate());

        Instant insertInstant = saved.getLastUpdate();

        Iterable<PscdImport> imports = pir.findAll();
        assertTrue(imports.iterator().hasNext(), "Insert and find should work");
        PscdImport selected = imports.iterator().next();
        assertEquals("name", selected.getName());
        selected.setName("updateName");

        pir.save(selected);
        Integer id = selected.getId();
        Optional<PscdImport> updated = pir.findById(id);

        updated.ifPresentOrElse(u -> assertEquals("updateName", u.getName()), () -> fail("Update failed"));
        updated.ifPresentOrElse(u -> assertNotEquals(0, u.getLastUpdate().compareTo(insertInstant)), () -> fail("Lastupdate is not updated"));

        pir.delete(updated.get());

        imports = pir.findAll();

        assertFalse(imports.iterator().hasNext(), "Delete failed");

    }

}
