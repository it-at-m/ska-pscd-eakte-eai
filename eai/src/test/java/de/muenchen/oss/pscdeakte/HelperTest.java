package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.helper.DateHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HelperTest {
    @Test
    void dateTest(){
        Assertions.assertEquals("2026-12-24T00:00:00.000+1:00", DateHelper.format("24.12.2026"));
    }
}
