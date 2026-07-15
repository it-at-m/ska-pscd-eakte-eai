package de.muenchen.oss.ska_pscd_eakte_eai.data;

import lombok.Data;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.springframework.stereotype.Component;

@Component
@Data
@CsvRecord( separator = ",", skipFirstLine = true) //TODO csv export evtl. ohne Titelzeile
public class PscdData {

    @DataField(pos = 1, required = true)
    String gpId;
    @DataField(pos = 2)
    String name;
    @DataField(pos = 3)
    String vorname;
    @DataField(pos = 4)
    String gebDat;
    @DataField(pos = 5)
    String zentralaktkennung;

}
