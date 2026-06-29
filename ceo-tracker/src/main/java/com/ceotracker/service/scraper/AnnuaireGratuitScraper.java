package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Random;

public class AnnuaireGratuitScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(AnnuaireGratuitScraper.class);
    private static final Random RANDOM = new Random();

    public AnnuaireGratuitScraper() {
        super("Annuaire-gratuit.ma", "ANNUAIRE_OFFICIEL");
    }

    @Override
    public List<CeoContact> scrape() {
        Set<CeoContact> results = new LinkedHashSet<>();
        String[] urls = {
            "https://www.annuaire-gratuit.ma/recherche/entreprise-ville-casablanca.html",
            "https://www.annuaire-gratuit.ma/recherche/societe-casablanca.html",
            "https://www.annuaire-gratuit.ma/recherche/annuaire-des-entreprises-ville-casablanca.html"
        };

        for (String url : urls) {
            try {
                log.info("AnnuaireGratuit: fetching {}", url);
                String html = Jsoup.connect(url)
                    .userAgent(randomUserAgent())
                    .timeout(15000)
                    .followRedirects(true)
                    .execute().body();

                List<PageParser.ScrapedEntry> entries = PageParser.extractEntries(html, url, "Casablanca");
                log.info("AnnuaireGratuit: {} numeros trouves", entries.size());

                for (PageParser.ScrapedEntry entry : entries) {
                    CeoContact contact = new CeoContact();
                    contact.setCompanyName(entry.companyName);
                    
                    contact.setPhoneNumber(entry.phoneNumber);
                    contact.setSourceUrl(url);
                    contact.setSourceType(getSourceType());
                    contact.setLastVerifiedAt(LocalDateTime.now());
                    results.add(contact);
                }
            } catch (Exception e) {
                log.warn("AnnuaireGratuit erreur: {}", e.getMessage());
            }
            sleep();
        }
        return new ArrayList<>(results);
    }

    private String randomUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36";
    }

    private void sleep() {
        try { Thread.sleep(2000 + RANDOM.nextInt(3000)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
