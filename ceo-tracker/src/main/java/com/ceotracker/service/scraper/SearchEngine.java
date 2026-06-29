package com.ceotracker.service.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SearchEngine {

    private static final Random RANDOM = new Random();

    public static List<SearchResult> search(String query, int maxResults) {
        // Bing is most reliable - try it first
        List<SearchResult> results = searchBing(query, maxResults);
        if (!results.isEmpty()) {
            return results;
        }

        // Fallbacks
        results = searchBrave(query, maxResults);
        if (!results.isEmpty()) return results;

        results = searchQwant(query, maxResults);
        if (!results.isEmpty()) return results;

        // DuckDuckGo as last resort (most blocked)
        results = searchDuckDuckGoLite(query, maxResults);
        return results;
    }

    private static List<SearchResult> searchBing(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://www.bing.com/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&count=" + maxResults;
            Document doc = Jsoup.connect(url)
                .userAgent(randomUA())
                .timeout(15000)
                .followRedirects(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .get();

            // Extract results from Bing's HTML
            for (Element li : doc.select("li.b_algo")) {
                try {
                    // Title and link
                    Element h2 = li.select("h2").first();
                    if (h2 == null) h2 = li.select("a").first();
                    String title = "";
                    String link = "";
                    if (h2 != null) {
                        title = h2.text().trim();
                        Element a = h2.select("a[href]").first();
                        if (a == null) a = h2;
                        if (a != null && a.hasAttr("href")) {
                            link = a.absUrl("href");
                        }
                    }

                    // If no link from h2, try any link
                    if (link.isEmpty()) {
                        Element anyA = li.select("a[href^=http]").first();
                        if (anyA != null) {
                            link = anyA.absUrl("href");
                            if (title.isEmpty()) title = anyA.text().trim();
                        }
                    }

                    // Snippet
                    String snippet = "";
                    Element caption = li.select(".b_caption p, .b_caption span").first();
                    if (caption != null) snippet = caption.text().trim();

                    if (!link.startsWith("http")) continue;
                    if (title.isEmpty()) continue;

                    results.add(new SearchResult(title, link, snippet));
                    if (results.size() >= maxResults) break;
                } catch (Exception ignored) {}
            }

            // If no results from specific selectors, try general link extraction
            if (results.isEmpty()) {
                for (Element a : doc.select("a[href^=http]")) {
                    try {
                        String href = a.absUrl("href");
                        String text = a.text().trim();
                        if (text.length() > 5 && !href.contains("bing.com") && !href.contains("microsoft")) {
                            // Find the nearest text after the link as snippet
                            Element parent = a.parent();
                            String snippet = "";
                            if (parent != null) {
                                Element pTag = parent.select("p, .b_caption p").first();
                                if (pTag != null) snippet = pTag.text().trim();
                            }
                            results.add(new SearchResult(text, href, snippet));
                            if (results.size() >= maxResults) break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Bing error: " + e.getMessage());
        }
        return results;
    }

    private static List<SearchResult> searchBrave(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://search.brave.com/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url)
                .userAgent(randomUA())
                .timeout(15000)
                .followRedirects(true)
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .get();

            for (Element a : doc.select("a[href^=http]")) {
                String href = a.absUrl("href");
                String text = a.text().trim();
                if (text.length() > 5 && !href.contains("brave.com") && !href.contains("google.com")) {
                    results.add(new SearchResult(text, href, ""));
                    if (results.size() >= maxResults) break;
                }
            }
        } catch (Exception e) {
            System.err.println("Brave error: " + e.getMessage());
        }
        return results;
    }

    private static List<SearchResult> searchQwant(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://www.qwant.com/?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&t=web";
            Document doc = Jsoup.connect(url)
                .userAgent(randomUA())
                .timeout(15000)
                .followRedirects(true)
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .get();

            for (Element a : doc.select("a[href^=http]")) {
                String href = a.absUrl("href");
                String text = a.text().trim();
                if (text.length() > 5 && !href.contains("qwant.com")) {
                    results.add(new SearchResult(text, href, ""));
                    if (results.size() >= maxResults) break;
                }
            }
        } catch (Exception e) {
            System.err.println("Qwant error: " + e.getMessage());
        }
        return results;
    }

    private static List<SearchResult> searchDuckDuckGoLite(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String url = "https://lite.duckduckgo.com/lite/?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url)
                .userAgent(randomUA())
                .timeout(20000)
                .followRedirects(true)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .get();

            for (Element a : doc.select("a[href^=http]")) {
                String href = a.absUrl("href");
                String text = a.text().trim();
                if (text.length() > 3 && !href.contains("duckduckgo.com")) {
                    results.add(new SearchResult(text, href, ""));
                    if (results.size() >= maxResults) break;
                }
            }
        } catch (Exception e) {
            System.err.println("DDG error: " + e.getMessage());
        }
        return results;
    }

    private static String randomUA() {
        String[] agents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7) AppleWebKit/605.1.15 Safari/605.1.15"
        };
        return agents[RANDOM.nextInt(agents.length)];
    }

    public static class SearchResult {
        public final String title;
        public final String url;
        public final String snippet;

        public SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
