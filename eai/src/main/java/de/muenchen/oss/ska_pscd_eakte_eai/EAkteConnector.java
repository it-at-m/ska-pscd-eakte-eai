package de.muenchen.oss.ska_pscd_eakte_eai;

import de.muenchen.oss.ska_pscd_eakte_eai.data.PscdData;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EAkteConnector implements Processor {

    private final PscdData pscd;

    @Override
    public void process(Exchange exchange) throws Exception {
//
    }
}
