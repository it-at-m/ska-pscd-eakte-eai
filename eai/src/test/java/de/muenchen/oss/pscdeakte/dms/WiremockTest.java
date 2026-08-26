package de.muenchen.oss.pscdeakte.dms;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class WiremockTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {

        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig().port(8080).withRootDirectory("../stack/wiremock"));
        wireMockServer.start();

    }

    @AfterEach
    void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}
