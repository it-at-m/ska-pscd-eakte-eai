package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.helper.DateHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HelperTest {
    @Test
    void dateTest(){
        Assertions.assertEquals("2026-12-24", DateHelper.format("24.12.2026"));
        Assertions.assertEquals("2026-08-26", DateHelper.format("26.08.2026"));
    }
}
