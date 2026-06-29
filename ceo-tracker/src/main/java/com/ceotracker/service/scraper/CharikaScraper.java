package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.PhoneNormalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharikaScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(CharikaScraper.class);
    private static final Random RANDOM = new Random();

    private static final Pattern PHONE_06_07 = Pattern.compile("0[67]\\d{8}");
    private static final Pattern RAISON_SOCIALE = Pattern.compile(
        "(?i)Raison sociale\\s*:?\\s*([A-Z][A-Za-z0-9éèêëàâäùûüôöîïç\\s'-]+)");
    private static final Pattern COMPANY_NAME = Pattern.compile(
        "([A-Z][A-Za-z0-9éèêëàâäùûüôöîïç]{3,}(?:\\s+[A-Z][A-Za-z0-9éèêëàâäùûüôöîïç]{3,}){1,4})");

    public CharikaScraper() {
        super("Charika.ma", "ANNUAIRE_OFFICIEL");
    }

    @Override
    public List<CeoContact> scrape() {
        Set<CeoContact> results = new LinkedHashSet<>();

        String[] searchUrls = {
            "https://www.charika.ma/societe?q=casablanca",
            "https://www.charika.ma/societe?q=casablanca&page=1",
        };

        for (String url : searchUrls) {
            try {
                String html = Jsoup.connect(url)
                    .userAgent(randomUA())
                    .timeout(15000)
                    .followRedirects(true)
                    .execute().body();

                // Extract entries from search page
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

                // Also extract company names from search page links
                Document doc = Jsoup.parse(html);
                for (var a : doc.select("a[href*=societe], a[href*=entreprise]")) {
                    String link = a.absUrl("href");
                    if (!link.startsWith("http")) continue;

                    // Visit each company profile page to find phone + company
                    String subHtml = fetchPage(link);
                    if (subHtml == null) continue;

                    String text = Jsoup.parse(subHtml).text();
                    String clean = text.replaceAll("[\\s\\-–—.()/]+", "");

                    // Find phone numbers
                    Matcher pm = PHONE_06_07.matcher(clean);
                    String phone = null;
                    if (pm.find()) {
                        phone = PhoneNormalizer.normalize(pm.group());
                    }
                    if (phone == null) continue;

                    // Find company name
                    String company = findCompanyName(text, subHtml);

                    CeoContact contact = new CeoContact();
                    contact.setPhoneNumber(phone);
                    contact.setCompanyName(company != null ? company : "Inconnue");
                    contact.setSourceUrl(link);
                    contact.setSourceType(getSourceType());
                    contact.setLastVerifiedAt(LocalDateTime.now());
                    results.add(contact);
                }
            } catch (Exception e) {
                log.debug("Charika erreur: {}", e.getMessage());
            }
            sleep(2000, 4000);
        }

        List<CeoContact> result = new ArrayList<>(results);
        log.info("Charika: {} contacts", result.size());
        return result;
    }

    private String findCompanyName(String text, String html) {
        // Look for "Raison sociale: XYZ"
        Matcher m = RAISON_SOCIALE.matcher(text);
        if (m.find()) {
            String name = m.group(1).trim();
            if (name.length() > 3 && name.length() < 100) return name;
        }
        // Fallback to page title
        Document doc = Jsoup.parse(html);
        String title = doc.title();
        if (title.length() > 3 && title.length() < 100) {
            return title.replaceAll("(?i)\\|.*", "").replaceAll("-.*", "").trim();
        }
        // Try any capitalized multi-word name
        Matcher nm = COMPANY_NAME.matcher(text);
        if (nm.find()) {
            String name = nm.group(1).trim();
            if (name.length() > 5 && name.length() < 60) return name;
        }
        return null;
    }

    private String fetchPage(String url) {
        try {
            return Jsoup.connect(url)
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
