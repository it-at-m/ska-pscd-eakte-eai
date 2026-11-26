package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.Objektreferenz;
import de.muenchen.oss.eakte.v1.generated.model.ReadApentryAntwortDTO;
import de.muenchen.oss.pscdeakte.configuration.annotation.LogExecutionTime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Apentries {

    private final DmsApentryService dmsService;
    private final Map<Integer, String> apentryMap = new ConcurrentHashMap<>();
    private final AtomicBoolean mapInitialized = new AtomicBoolean(false);
    private final ReadWriteLock mapLock = new ReentrantReadWriteLock(true);
    private final Pattern pattern;
    private final DmsProperties props;

    public Apentries(final DmsApentryService dmsService, final DmsProperties properties) {
        this.dmsService = dmsService;
        this.props = properties;
        pattern = Pattern.compile(Pattern.quote(properties.getAktenplannummer()) + "\\.([0-9]+)/[0-9]{10}-[0-9]{10}");
    }

    @LogExecutionTime
    public void initMap() {
        mapLock.writeLock().lock();
        try {
            if (props.getInitialbefuellung() && !mapInitialized.get()) {
                log.info("reading apentries");
                final ReadApentryAntwortDTO response = dmsService.getApentries();
                final List<Objektreferenz> giObjects = response != null ? response.getGiobjecttype() : null;
                if (giObjects != null) {
                    log.info("{} apentries found", giObjects.size());
                    giObjects.forEach(ref -> fillMap(ref, apentryMap));
                }
                mapInitialized.set(true);
            }
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    /**
     * Liest die Map bei Initialbefüllung erneut aus der eAkte. Bei einem fehlgeschlagenen
     * Lesevorgang bleibt der bisherige Inhalt erhalten.
     */
    @LogExecutionTime
    public void refreshMap() {
        mapLock.writeLock().lock();
        try {
            if (!props.getInitialbefuellung()) {
                return;
            }
            final ReadApentryAntwortDTO response = dmsService.getApentries();
            final List<Objektreferenz> giObjects = response != null ? response.getGiobjecttype() : null;
            if (giObjects == null) {
                throw new IllegalStateException("Betreffseinheiten konnten nicht erneut geladen werden");
            }
            final Map<Integer, String> refreshedMap = new ConcurrentHashMap<>();
            giObjects.forEach(ref -> fillMap(ref, refreshedMap));
            apentryMap.clear();
            apentryMap.putAll(refreshedMap);
            mapInitialized.set(true);
        } finally {
            mapLock.writeLock().unlock();
        }
    }

    public String getApentryCoo(final String gpId) {
        final int lfdNr = this.generateLfdNr(gpId);
        if (props.getInitialbefuellung()) {
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
        mapLock.readLock().lock();
        try {
            return apentryMap.computeIfAbsent(lfdNr, this::getNewApentry);
        } finally {
            mapLock.readLock().unlock();
        }
    }

    private String getNewApentry(final Integer lfdNr) {
        log.debug("creating new apentry");
        final DmsObjektResponse response = dmsService.createSubjectAreaUnit(lfdNr, this.buildObjname(lfdNr));
        log.debug("new apentry name: {} coo: {}", response.getObjname(), response.getObjid());
        return response.getObjid();
    }

    private void fillMap(final Objektreferenz ref, final Map<Integer, String> map) {
        final String objname = ref.getObjname();
        if (objname != null && !objname.isEmpty()) {
            final Matcher matcher = pattern.matcher(objname);
            if (matcher.find()) {
                final String objaddress = ref.getObjaddress();
                log.debug("saving apentry name: {} coo: {}", ref.getObjname(), objaddress);
                final int lfdNr = Integer.parseInt(matcher.group(1));
                map.put(lfdNr, objaddress);
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
