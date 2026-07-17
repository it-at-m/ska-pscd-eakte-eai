package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.dms.DmsService;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("eAkteConnector")
public class EAkteConnector implements Processor {

    private final DmsService dmsService;

    @Override
    public void process(Exchange exchange) throws Exception {

        ReadApentryAntwortDTO response = dmsService.getApentries();

    }
}
