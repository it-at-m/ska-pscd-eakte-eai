package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.data.PscdData;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component("eAkteConnector")
public class EAkteConnector implements Processor {

    private final DmsService dmsService;
    private final Apentries apentries;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final PscdData data = exchange.getIn().getBody(PscdData.class);
        if (data == null) {
            log.error("data is null");
            return;
        }
        log.info("processing gpid {}", data.getGpId());
        String apentryCoo = apentries.getApentryCoo(data.getGpId());
        log.info("saving gp {} into apentry {}", data.getGpId(), apentryCoo);
        log.info("creating file");
        String filecoo = dmsService.createFile(data, apentryCoo).getObjid();
        log.info("file {} created", filecoo);
        log.info("creating procedures");
        String procedurecoo = dmsService.createProcedureBestandsakte(filecoo).getObjid();
        log.info("Bestandsakten created: {}", procedurecoo);
        procedurecoo = dmsService.createProcedureAV(filecoo).getObjid();
        log.info("AVs, Titel, Haftbefehle created: {}", procedurecoo);
    }
}
