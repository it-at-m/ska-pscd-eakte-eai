package de.muenchen.oss.pscdeakte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.util.List;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The test is used to demonstrate how any test configuration
 * (test/application-test.yml) to test the
 * entire EAI can be tested from start to shut down with one test call.
 **/
@SpringBootTest
@CamelSpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class EaiTest {

    @EndpointInject("mock:example")
    private MockEndpoint output;

    @Test
    void givenMessage_thenSendToMockShouldSucceed() throws InterruptedException {

        output.expectedMessageCount(1);

        output.assertIsSatisfied();

//        List<PscdImport> imports = output.getExchanges().getFirst().getMessage().getBody(List.class);
//        assertEquals(10, imports.size());

    }

}
