package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class PagesMarocScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(PagesMarocScraper.class);
    private static final Random RANDOM = new Random();

    public PagesMarocScraper() {
        super("Pages-Maroc.com", "PAGES_JAUNES");
    }

    @Override
    public List<CeoContact> scrape() {
        List<CeoContact> results = new ArrayList<>();

        String[] queries = {
            "site:pages-maroc.com entreprise Casablanca téléphone",
            "site:pages-maroc.com société Casablanca contact",
            "site:pages-maroc.com annuaire Casablanca portable",
            "site:pagesmaroc.com Casablanca téléphone entreprise"
        };

        for (String query : queries) {
            try {
                List<SearchEngine.SearchResult> searchResults = SearchEngine.search(query, 15);

                for (SearchEngine.SearchResult sr : searchResults) {
                    if (!sr.url.contains("pages-maroc") && !sr.url.contains("pagesmaroc")) continue;

                    String html = fetchViaCache(sr.url);
                    if (html == null) continue;

                    List<PageParser.ScrapedEntry> entries = PageParser.extractEntries(html, sr.url, "Casablanca");
                    for (PageParser.ScrapedEntry entry : entries) {
                        String phone = PhoneNormalizer.normalize(entry.phoneNumber);
                        if (phone != null && (phone.startsWith("06") || phone.startsWith("07"))) {
                            CeoContact contact = new CeoContact();
                            contact.setPhoneNumber(phone);
                            contact.setCompanyName(entry.companyName);
                            
                            contact.setSourceUrl(sr.url);
                            contact.setSourceType(getSourceType());
                            contact.setLastVerifiedAt(LocalDateTime.now());
                            results.add(contact);
                        }
                    }
                    sleep(2000, 4000);
                }
            } catch (Exception e) {
                log.debug("PagesMaroc search error: {}", e.getMessage());
            }
        }

        log.info("Pages-Maroc: {} contacts trouves", results.size());
        return results;
    }

    private String fetchViaCache(String url) {
        try {
            String cacheUrl = "https://webcache.googleusercontent.com/search?q=cache:" +
                java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8);
            return org.jsoup.Jsoup.connect(cacheUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0")
                .timeout(10000)
                .followRedirects(true)
                .execute().body();
        } catch (Exception ignored) {}
        try {
            return org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0")
                .timeout(10000)
                .followRedirects(true)
                .execute().body();
        } catch (Exception ignored) {}
        return null;
    }

    private void sleep(int min, int max) {
        try { Thread.sleep(min + RANDOM.nextInt(max - min)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
