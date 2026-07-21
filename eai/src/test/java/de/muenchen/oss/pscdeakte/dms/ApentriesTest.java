package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class ApentriesTest {

    @Autowired
    Apentries apentries;

    @Test
    void objektnameTest(){
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "9512.1/1000000001-1000005000");
        map.put(2, "9512.2/1000005001-1000010000");
        map.put(3, "9512.3/1000010001-1000015000");
        map.put(4, "9512.4/1000015001-1000020000");
        for (Map.Entry<Integer, String> entry : map.entrySet()){
            assertEquals(entry.getValue(), apentries.buildObjname(entry.getKey()));
        }
    }

    @Test
    void IntegerTest(){
        Map<String, Integer> map = new HashMap<>();
        map.put("1000000000", 1);
        map.put("1000000001", 1);
        map.put("1000004999", 1);
        map.put("1000005000", 1);
        map.put("1000005001", 2);
        map.put("1000009999", 2);
        map.put("1000010000", 2);
        map.put("1000010001", 3);
        map.put("1000034569", 7);
        for (Map.Entry<String, Integer> entry : map.entrySet()){
            assertEquals(entry.getValue(), apentries.generateLfdNr(entry.getKey()));
        }
    }

}
