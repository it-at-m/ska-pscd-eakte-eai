package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@RequiredArgsConstructor
@Component
public class Apentries {

    @Autowired
    public Apentries(DmsService dmsService, DmsProperties properties) {
        this.dmsService = dmsService;
        this.dmsProperties = properties;
//        wg. Testsystem
        pattern = Pattern.compile(Pattern.quote(dmsProperties.getAktenplannummer()) + "\\.([0-9]+)/[0-9]{10}-[0-9]{10}");
    }

    private final DmsService dmsService;
    private final DmsProperties dmsProperties;
    private final Map<Integer, String> apentryMap = new ConcurrentHashMap<>();
    private volatile boolean mapInitialized = false;
    private final Pattern pattern;

    public String getApentryCoo(String gpId) {
        if (!mapInitialized) {
            synchronized (this) {
                if (!mapInitialized) {
                    log.info("reading apentries");
                    ReadApentryAntwortDTO response = dmsService.getApentries();
                    if (response.getGiobjecttype() != null) {
                        log.info("{} apentries found", response.getGiobjecttype().size());
                        response.getGiobjecttype().forEach(this::fillMap);
                    }
                    mapInitialized = true;
                }
            }
        }
        int lfdnr = this.generateLfdNr(gpId);
        return apentryMap.computeIfAbsent(lfdnr, newLfdnr -> {
            log.info("creating new apentry");
            DmsObjektResponse response = dmsService.createSubjectAreaUnit(newLfdnr, this.buildObjname(newLfdnr));
            log.info("new apentry name: {} coo: {}", response.getObjname(), response.getObjid());
            return response.getObjid();
        });
    }

    private void fillMap(Objektreferenz ref) {
        String objname = ref.getObjname();
//        komplex wegen Testsystem
        Matcher matcher;
        if (objname != null && !objname.isEmpty() && (matcher = pattern.matcher(objname)).find()) {
            log.info("saving apentry name: {} coo: {}", ref.getObjname(), ref.getObjaddress());
            apentryMap.put(Integer.parseInt(matcher.group(1)), ref.getObjaddress());
        } else {
            log.warn("apentry does not match criteria: {}", ref.getObjname());
        }
    }

    public int generateLfdNr(String gpId) {
        return (Integer.parseInt(gpId) - 1000000001) / 5000;
    }

    public String buildObjname(int lfdNr) {
        int mrd = 1000000000;
        int begin = mrd + 1 + lfdNr * 5000;
        int end = mrd + (lfdNr + 1) * 5000;
        return dmsProperties.getAktenplannummer() + "." + lfdNr + "/" + begin + "-" + end;
    }

}
