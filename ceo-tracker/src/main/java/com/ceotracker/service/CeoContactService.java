package com.ceotracker.service;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.repository.CeoContactRepository;
import com.ceotracker.service.scraper.PdfScraper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CeoContactService {

    private static final Logger log = LoggerFactory.getLogger(CeoContactService.class);

    private final CeoContactRepository repository;

    public CeoContactService(CeoContactRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void loadPdfOnStartup() {
        log.info("Chargement des donnees PDF au demarrage...");
        PdfScraper pdfScraper = new PdfScraper();
        try {
            List<CeoContact> contacts = pdfScraper.scrape();
            int saved = 0;
            for (CeoContact c : contacts) {
                if (saveIfNew(c)) saved++;
            }
            log.info("PDF: {} contacts charges ({} nouveaux)", contacts.size(), saved);
        } catch (Exception e) {
            log.warn("Erreur chargement PDF: {}", e.getMessage());
        }
    }

    public List<CeoContact> getAll() {
        return repository.findAll();
    }

    public List<CeoContact> getByCity(String city) {
        if (city == null || city.isBlank() || city.equals("TOUTES")) {
            return repository.findAll();
        }
        return repository.findByCityOrderByViabilityScoreDesc(city);
    }

    public List<String> getAvailableCities() {
        List<String> cities = repository.findDistinctCities();
        List<String> sorted = new ArrayList<>(cities);
        // Casablanca en premier
        sorted.sort((a, b) -> {
            if (a.equals("CASABLANCA")) return -1;
            if (b.equals("CASABLANCA")) return 1;
            return a.compareTo(b);
        });
        return sorted;
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
                if (changed) {
                    ec.setLastVerifiedAt(LocalDateTime.now());
                    repository.save(ec);
                }
                return false;
            } else {
                contact.setLastVerifiedAt(LocalDateTime.now());
                repository.save(contact);
                return true;
            }
        } catch (Exception e) {
            log.debug("Erreur sauvegarde: {}", e.getMessage());
            return false;
        }
    }

    public void updateStatus(Long contactId, String status) {
        if (!List.of("valide", "pending", "refus").contains(status)) {
            log.warn("Statut invalide: {}", status);
            return;
        }
        repository.findById(contactId).ifPresent(c -> {
            c.setStatus(status);
            repository.save(c);
            log.debug("Statut mis a jour: {} -> {}", contactId, status);
        });
    }

    public long count() {
        return repository.count();
    }
}
