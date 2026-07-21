package de.muenchen.oss.pscdeakte;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class MyTests {

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
            assertEquals(entry.getValue(), (Integer.parseInt(entry.getKey()) - 1000000001) / 5000 + 1);
        }
    }

    public String buildObjname(int lfdNr){
        int first = 1000000000;
        int begin = first + (lfdNr - 1) * 5000 + 1;
        int end = first + lfdNr * 5000;
        return "9512." + lfdNr + "/" + begin + "-" + end;
    }

    @Test
    void objNameTest(){
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "9512.1/1000000001-1000005000");
        map.put(2, "9512.2/1000005001-1000010000");
        map.put(3, "9512.3/1000010001-1000015000");
        map.put(4, "9512.4/1000015001-1000020000");
        for (Map.Entry<Integer, String> entry : map.entrySet()){
            assertEquals(entry.getValue(), this.buildObjname(entry.getKey()));
        }
    }

    @Test
    void regexTest(){
        Map<String, Boolean> map = new HashMap<>();
        map.put("9512.1/1000000001-1000005000", true);
        map.put("9512.2/1000005001-1000010000", true);
        map.put("9512.3/1000010001-1000015000", true);
        map.put("9512.4/1000015001-1000020000", true);
        map.put("9513.4/1000015001-1000020000", false);
        map.put("9512.4/1000015001 - 1000020000", false);
        map.put("null187/1000935001-1000940000", false);
        map.put("9512/Test Vererbung", false);

        Pattern pattern = Pattern.compile("9512\\.[0-9]+/[0-9]{10}-[0-9]{10}");
        for (Map.Entry<String, Boolean> entry : map.entrySet()){
            assertEquals(entry.getValue(), pattern.matcher(entry.getKey()).find());
        }
    }

}
