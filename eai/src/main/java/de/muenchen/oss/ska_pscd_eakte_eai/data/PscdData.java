package de.muenchen.oss.ska_pscd_eakte_eai.data;

import lombok.Data;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

@Data
@CsvRecord( separator = ",", skipFirstLine = true)
public class PscdData {

    @DataField(pos = 1)
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
