package com.ceotracker.service;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.repository.CeoContactRepository;
import com.ceotracker.service.scraper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class CeoContactService {

    private static final Logger log = LoggerFactory.getLogger(CeoContactService.class);
    private static final long SCRAPE_DURATION_MINUTES = 5;

    private final CeoContactRepository repository;
    private final ScrapingEventService eventService;
    private final List<BaseScraper> scrapers;

    private volatile boolean scrapingActive = false;

    public CeoContactService(CeoContactRepository repository, ScrapingEventService eventService) {
        this.repository = repository;
        this.eventService = eventService;
        this.scrapers = new ArrayList<>();
        this.scrapers.add(new PdfScraper());
        this.scrapers.add(new AnnuaireGratuitScraper());
        this.scrapers.add(new CharikaScraper());
        this.scrapers.add(new MassSiteScraper());
        this.scrapers.add(new TelecontactScraper());
        this.scrapers.add(new InfoContactScraper());
        this.scrapers.add(new PagesMarocScraper());
    }

    public List<CeoContact> getAll() {
        return repository.findAll();
    }

    public List<String> getSourceTypes() {
        return repository.findDistinctSourceTypes();
    }

    public boolean isScrapingActive() {
        return scrapingActive;
    }

    public void runScraping() {
        if (scrapingActive) {
            log.warn("Scraping déjà en cours");
            return;
        }
        scrapingActive = true;
        long startCount = repository.count();

        log.info("=== SCRAPING DEMARRE pour {} minutes ===", SCRAPE_DURATION_MINUTES);
        eventService.sendProgress("Démarrage", 0);

        Instant endTime = Instant.now().plus(Duration.ofMinutes(SCRAPE_DURATION_MINUTES));
        int cycle = 0;

        while (Instant.now().isBefore(endTime) && scrapingActive) {
            cycle++;
            long remaining = Duration.between(Instant.now(), endTime).toSeconds();
            log.info("--- Cycle {} / {}s restantes ---", cycle, remaining);
            eventService.sendProgress("Cycle " + cycle + " (" + remaining + "s)", 0);

            for (BaseScraper scraper : scrapers) {
                if (Instant.now().isAfter(endTime) || !scrapingActive) break;
                try {
                    List<CeoContact> contacts = scraper.scrape();
                    int saved = 0;
                    for (CeoContact contact : contacts) {
                        if (saveIfNew(contact)) saved++;
                    }
                    if (saved > 0) {
                        log.info("+{} via {} (total: {})", saved, scraper.getName(), repository.count());
                    }
                } catch (Exception e) {
                    log.debug("Erreur {}: {}", scraper.getName(), e.getMessage());
                }
            }

            // Brief pause between cycles to let other scrapers breathe
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }

        long finalCount = repository.count();
        eventService.sendTotal((int) finalCount);
        eventService.sendDone();
        log.info("=== SCRAPING TERMINE apres {} minutes: {} total ({} nouveaux) ===",
            SCRAPE_DURATION_MINUTES, finalCount, finalCount - startCount);
        scrapingActive = false;
    }

    public void stopScraping() {
        scrapingActive = false;
    }

    private synchronized boolean saveIfNew(CeoContact contact) {
        try {
            Optional<CeoContact> existing = repository.findByPhoneNumber(contact.getPhoneNumber());
            if (existing.isPresent()) {
                CeoContact ec = existing.get();
                boolean changed = false;
                if (contact.getCompanyName() != null && !contact.getCompanyName().equals("Inconnue")
                    && !contact.getCompanyName().isBlank()
                    && (ec.getCompanyName() == null || ec.getCompanyName().equals("Inconnue"))) {
                    ec.setCompanyName(contact.getCompanyName());
                    changed = true;
                }
                if (contact.getSourceUrl() != null && !contact.getSourceUrl().equals(ec.getSourceUrl())) {
                    ec.setSourceUrl(contact.getSourceUrl());
                    ec.setNumberOfSources(ec.getNumberOfSources() + 1);
                    changed = true;
                }
                if (changed) {
                    ec.setLastVerifiedAt(LocalDateTime.now());
                    repository.save(ec);
                }
                return false;
            } else {
                contact.setLastVerifiedAt(LocalDateTime.now());
                contact.setNumberOfSources(1);
                repository.save(contact);
                eventService.sendContact(contact);
                return true;
            }
        } catch (Exception e) {
            log.debug("Erreur sauvegarde: {}", e.getMessage());
            return false;
        }
    }

    public long count() {
        return repository.count();
    }
}
