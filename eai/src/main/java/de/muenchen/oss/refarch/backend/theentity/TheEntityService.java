package de.muenchen.oss.refarch.backend.theentity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TheEntityService {

    @Scheduled(cron = "${dms.cron}")
    public void refresh() {
        log.warn("Scheduler is running...");
    }
}
