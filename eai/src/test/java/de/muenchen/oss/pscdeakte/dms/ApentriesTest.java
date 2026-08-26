package de.muenchen.oss.pscdeakte.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.pscdeakte.TestConstants;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Disabled
class ApentriesTest {

    @Autowired
    Apentries apentries;

    @Test
    void objektnameTest() {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "1000000001-1000005000");
        map.put(1, "1000005001-1000010000");
        map.put(2, "1000010001-1000015000");
        map.put(3, "1000015001-1000020000");
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            assertEquals(entry.getValue(), apentries.buildObjname(entry.getKey()));
        }
    }

    @Test
    void integerTest() {
        Map<String, Integer> map = new HashMap<>();
        map.put("1000000000", 0);
        map.put("1000000001", 0);
        map.put("1000004999", 0);
        map.put("1000005000", 0);
        map.put("1000005001", 1);
        map.put("1000009999", 1);
        map.put("1000010000", 1);
        map.put("1000010001", 2);
        map.put("1000034569", 6);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            assertEquals(entry.getValue(), apentries.generateLfdNr(entry.getKey()));
        }
    }

}
