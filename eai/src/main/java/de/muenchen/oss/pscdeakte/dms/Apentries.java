package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class Apentries {

    public Apentries(final DmsService dmsService, final DmsProperties properties) {
        this.dmsService = dmsService;
        this.props = properties;
        pattern = Pattern.compile(Pattern.quote(properties.getAktenplannummer()) + "\\.([0-9]+)/[0-9]{10}-[0-9]{10}");
    }

    private final DmsService dmsService;
    private final Map<Integer, String> apentryMap = new ConcurrentHashMap<>();
    private volatile boolean mapInitialized = false;
    private final Pattern pattern;
    private final DmsProperties props;

    public String getApentryCoo(final String gpId) {
        final int lfdNr = this.generateLfdNr(gpId);
        if (props.isInitialbefuellung()) {
            return getApentryFromMap(lfdNr);
        } else {
            return this.getSingleApentry(lfdNr);
        }
    }

    private String getSingleApentry(final int lfdNr) {
        List<Objektreferenz> obj = dmsService.getApentryFor(lfdNr).getGiobjecttype();
        if (obj == null || obj.isEmpty()) {
            return this.getNewApentry(lfdNr);
        } else {
            return obj.getFirst().getObjaddress();
        }
    }

    public String getApentryFromMap(final int lfdNr) {
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
        return apentryMap.computeIfAbsent(lfdNr, this::getNewApentry);
    }

    private String getNewApentry(Integer lfdNr) {
        log.info("creating new apentry");
        DmsObjektResponse response = dmsService.createSubjectAreaUnit(lfdNr, this.buildObjname(lfdNr));
        log.info("new apentry name: {} coo: {}", response.getObjname(), response.getObjid());
        return response.getObjid();
    }

    private void fillMap(final Objektreferenz ref) {
        final String objname = ref.getObjname();
        Matcher matcher;
        if (objname != null && !objname.isEmpty() && (matcher = pattern.matcher(objname)).find()) {
            final String objaddress = ref.getObjaddress();
            log.info("saving apentry name: {} coo: {}", ref.getObjname(), objaddress);
            final int lfdNr = Integer.parseInt(matcher.group(1));
            apentryMap.put(lfdNr, objaddress);
        } else {
            log.warn("apentry does not match criteria: {}", ref.getObjname());
        }
    }

    public int generateLfdNr(final String gpId) {
        return (Integer.parseInt(gpId) - 1000000001) / 5000;
    }

    public String buildObjname(final int lfdNr) {
        final int mrd = 1000000000;
        final int begin = mrd + 1 + lfdNr * 5000;
        final int end = mrd + (lfdNr + 1) * 5000;
        return begin + "-" + end;
    }

}
