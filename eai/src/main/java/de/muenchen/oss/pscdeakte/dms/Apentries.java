package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class Apentries {

    private final DmsService dmsService;
    private final Map<Integer, String> apentryMap = new ConcurrentHashMap<>();
    private final AtomicBoolean mapInitialized = new AtomicBoolean(false);
    private static final Lock LOCK = new ReentrantLock(true);
    private final Pattern pattern;
    private final DmsProperties props;

    public Apentries(final DmsService dmsService, final DmsProperties properties) {
        this.dmsService = dmsService;
        this.props = properties;
        pattern = Pattern.compile(Pattern.quote(properties.getAktenplannummer()) + "\\.([0-9]+)/[0-9]{10}-[0-9]{10}");
    }

    public String getApentryCoo(final String gpId) {
        final int lfdNr = this.generateLfdNr(gpId);
        if (props.isInitialbefuellung()) {
            return getApentryFromMap(lfdNr);
        } else {
            return this.getSingleApentry(lfdNr);
        }
    }

    private String getSingleApentry(final int lfdNr) {
        final List<Objektreferenz> obj = dmsService.getApentryFor(lfdNr).getGiobjecttype();
        if (obj == null || obj.isEmpty()) {
            return this.getNewApentry(lfdNr);
        } else {
            return obj.getFirst().getObjaddress();
        }
    }

    private String getApentryFromMap(final int lfdNr) {
        LOCK.lock();
        try {
            if (!mapInitialized.get()) {
                log.info("reading apentries");
                final ReadApentryAntwortDTO response = dmsService.getApentries();
                final List<Objektreferenz> giObjects = response != null ? response.getGiobjecttype() : null;
                if (giObjects != null) {
                    log.info("{} apentries found", giObjects.size());
                    giObjects.forEach(this::fillMap);
                }
                mapInitialized.set(true);
            }
        } finally {
            LOCK.unlock();
        }
        return apentryMap.computeIfAbsent(lfdNr, this::getNewApentry);
    }

    private String getNewApentry(final Integer lfdNr) {
        log.debug("creating new apentry");
        final DmsObjektResponse response = dmsService.createSubjectAreaUnit(lfdNr, this.buildObjname(lfdNr));
        log.debug("new apentry name: {} coo: {}", response.getObjname(), response.getObjid());
        return response.getObjid();
    }

    private void fillMap(final Objektreferenz ref) {
        final String objname = ref.getObjname();
        if (objname != null && !objname.isEmpty()) {
            final Matcher matcher = pattern.matcher(objname);
            if (matcher.find()) {
                final String objaddress = ref.getObjaddress();
                log.debug("saving apentry name: {} coo: {}", ref.getObjname(), objaddress);
                final int lfdNr = Integer.parseInt(matcher.group(1));
                apentryMap.put(lfdNr, objaddress);
            } else {
                log.debug("apentry does not match criteria: {}", ref.getObjname());
            }
        }
    }

    protected int generateLfdNr(final String gpId) {
        return (Integer.parseInt(gpId) - 1_000_000_001) / 5000;
    }

    protected String buildObjname(final int lfdNr) {
        final int mrd = 1_000_000_000;
        final int begin = mrd + 1 + lfdNr * 5000;
        final int end = mrd + (lfdNr + 1) * 5000;
        return begin + "-" + end;
    }

}
