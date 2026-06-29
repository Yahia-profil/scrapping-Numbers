package com.ceotracker.scheduler;

import com.ceotracker.service.CeoContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScrapingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapingScheduler.class);

    private final CeoContactService ceoContactService;

    public ScrapingScheduler(CeoContactService ceoContactService) {
        this.ceoContactService = ceoContactService;
    }

    @Scheduled(cron = "0 0 */2 * * *")
    public void scheduledScraping() {
        log.info("Scheduler: lancement du scraping automatique toutes les 2h");
        ceoContactService.runScraping();
    }
}
