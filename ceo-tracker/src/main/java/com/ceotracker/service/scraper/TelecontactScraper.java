package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.PhoneNormalizer;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class TelecontactScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(TelecontactScraper.class);
    private static final Random RANDOM = new Random();

    private static final String[] DIRECT_URLS = {
        "https://www.telecontact.ma/annuaire/casablanca/entreprises/",
        "https://www.telecontact.ma/recherche/casablanca/",
    };

    public TelecontactScraper() {
        super("Telecontact.ma", "PAGES_JAUNES");
    }

    @Override
    public List<CeoContact> scrape() {
        Set<CeoContact> results = new LinkedHashSet<>();

        // Try direct URLs first
        for (String url : DIRECT_URLS) {
            try {
                String html = Jsoup.connect(url)
                    .userAgent(randomUA())
                    .timeout(15000)
                    .followRedirects(true)
                    .execute().body();
                List<PageParser.ScrapedEntry> entries = PageParser.extractEntries(html, url, "Casablanca");
                for (PageParser.ScrapedEntry entry : entries) {
                    CeoContact contact = new CeoContact();
                    contact.setPhoneNumber(entry.phoneNumber);
                    contact.setCompanyName(entry.companyName);
                    contact.setSourceUrl(url);
                    contact.setSourceType(getSourceType());
                    contact.setLastVerifiedAt(LocalDateTime.now());
                    results.add(contact);
                }
            } catch (Exception ignored) {}
            sleep(2000, 4000);
        }

        // Fallback: search for Telecontact pages via search engine
        if (results.isEmpty()) {
            String[] queries = {
                "site:telecontact.ma entreprise Casablanca téléphone",
                "site:telecontact.ma société Casablanca mobile",
            };
            for (String query : queries) {
                try {
                    List<SearchEngine.SearchResult> searchResults = SearchEngine.search(query, 15);
                    for (SearchEngine.SearchResult sr : searchResults) {
                        if (!sr.url.contains("telecontact.ma")) continue;
                        String html = fetchViaCache(sr.url);
                        if (html == null) continue;
                        List<PageParser.ScrapedEntry> entries = PageParser.extractEntries(html, sr.url, "Casablanca");
                        for (PageParser.ScrapedEntry entry : entries) {
                            CeoContact contact = new CeoContact();
                            contact.setPhoneNumber(entry.phoneNumber);
                            contact.setCompanyName(entry.companyName);
                            contact.setSourceUrl(sr.url);
                            contact.setSourceType(getSourceType());
                            contact.setLastVerifiedAt(LocalDateTime.now());
                            results.add(contact);
                        }
                        sleep(2000, 4000);
                    }
                } catch (Exception ignored) {}
            }
        }

        List<CeoContact> result = new ArrayList<>(results);
        log.info("Telecontact: {} contacts", result.size());
        return result;
    }

    private String fetchViaCache(String url) {
        try {
            String cacheUrl = "https://webcache.googleusercontent.com/search?q=cache:" +
                java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8);
            return Jsoup.connect(cacheUrl)
                .userAgent(randomUA())
                .timeout(10000)
                .followRedirects(true)
                .execute().body();
        } catch (Exception ignored) {}
        return null;
    }

    private String randomUA() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36";
    }

    private void sleep(int min, int max) {
        try { Thread.sleep(min + RANDOM.nextInt(max - min)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
