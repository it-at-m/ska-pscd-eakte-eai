package de.muenchen.oss.pscdeakte.s3;

import de.muenchen.oss.pscdeakte.TestConstants;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Slf4j
class S3Test {

    private final S3OutPort s3OutPort;
    private final S3Properties props;

    @Autowired
    S3Test(S3OutPort s3, S3Properties props){
        this.s3OutPort = s3;
        this.props = props;
    }

    @Test
    void s3Test() throws S3Exception, IOException {

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
}
