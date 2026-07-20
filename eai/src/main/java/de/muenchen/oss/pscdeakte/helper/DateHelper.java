package de.muenchen.oss.pscdeakte.helper;

public class DateHelper {

    private DateHelper(){
//        only static
    }

    public static String format(String date){
        return date.substring(6) + "-" + date.substring(3, 5) + "-" + date.substring(0, 2) + "T00:00:00.000+01:00";
    }
}
