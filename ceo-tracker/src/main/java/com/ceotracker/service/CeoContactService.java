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

    public List<CeoContact> getByActivity(String activity) {
        return repository.findByActivityOrderByViabilityScoreDesc(activity);
    }

    public List<CeoContact> getByCityAndActivity(String city, String activity) {
        if ((city == null || city.isBlank() || city.equals("TOUTES"))
            && (activity == null || activity.isBlank() || activity.equals("TOUTES"))) {
            return repository.findAll();
        }
        if (city == null || city.isBlank() || city.equals("TOUTES")) {
            return repository.findByActivityOrderByViabilityScoreDesc(activity);
        }
        if (activity == null || activity.isBlank() || activity.equals("TOUTES")) {
            return repository.findByCityOrderByViabilityScoreDesc(city);
        }
        return repository.findByCityAndActivityOrderByViabilityScoreDesc(city, activity);
    }

    public List<String> getAvailableCities() {
        List<String> cities = repository.findDistinctCities();
        List<String> sorted = new ArrayList<>(cities);
        sorted.sort((a, b) -> {
            if (a.equals("CASABLANCA")) return -1;
            if (b.equals("CASABLANCA")) return 1;
            return a.compareTo(b);
        });
        return sorted;
    }

    public List<String> getAvailableActivities() {
        return repository.findDistinctActivities();
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
                if (contact.getActivity() != null && !contact.getActivity().isBlank()
                    && (ec.getActivity() == null || ec.getActivity().isBlank())) {
                    ec.setActivity(contact.getActivity());
                    changed = true;
                }
                if (contact.getJobTitle() != null && !contact.getJobTitle().isBlank()
                    && (ec.getJobTitle() == null || ec.getJobTitle().isBlank())) {
                    ec.setJobTitle(contact.getJobTitle());
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

    public List<CeoContact> search(String city, String activity, String q) {
        return search(city, activity, q, "TOUS");
    }

    public List<CeoContact> search(String city, String activity, String q, String status) {
        List<CeoContact> filtered = getByCityAndActivity(city, activity);
        if (status != null && !status.isBlank() && !status.equals("TOUS")) {
            filtered = filtered.stream()
                .filter(c -> status.equals(c.getStatus()))
                .toList();
        }
        if (q == null || q.isBlank()) return filtered;
        String lower = q.toLowerCase();
        return filtered.stream()
            .filter(c -> (c.getCompanyName() != null && c.getCompanyName().toLowerCase().contains(lower))
                      || (c.getCeoName() != null && c.getCeoName().toLowerCase().contains(lower))
                      || (c.getActivity() != null && c.getActivity().toLowerCase().contains(lower)))
            .toList();
    }

    public void updateNotes(Long contactId, String notes) {
        repository.findById(contactId).ifPresent(c -> {
            c.setNotes(notes);
            repository.save(c);
        });
    }

    public long count() {
        return repository.count();
    }
}
