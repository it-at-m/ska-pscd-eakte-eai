package de.muenchen.oss.pscdeakte.helper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateHelper {

    private DateHelper(){
//        only static
    }

    public static String format(String date){
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy")).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
