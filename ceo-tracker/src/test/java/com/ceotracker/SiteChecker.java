package com.ceotracker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

public class SiteChecker {

    private static final List<String> URLS = List.of(
            "https://www.telecontact.ma/",
            "https://www.telecontact.ma/liens/entreprises/casablanca.php",
            "https://www.kerix.net/fr/",
            "https://infocontact.ma/",
            "https://www.annuaire-gratuit.ma/",
            "https://www.pages-maroc.com/",
            "https://ma.kompass.com/",
            "https://www.annuaire.ma/",
            "https://www.charika.ma/"
    );

    public static void main(String[] args) {
        System.out.printf("%-50s | %-12s | %-16s | %-40s | %s%n",
                "URL", "Status Code", "Content Length", "Page Title", "First 200 chars");
        System.out.println("-".repeat(200));

        for (String url : URLS) {
            checkUrl(url);
        }
    }

    private static void checkUrl(String url) {
        String status;
        int contentLength = -1;
        String title = "";
        String firstChars = "";

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            status = String.valueOf(doc.connection().response().statusCode());
            contentLength = doc.text().length();
            title = doc.title();
            String text = doc.body().text();
            firstChars = text.length() > 200 ? text.substring(0, 200) : text;

        } catch (SocketTimeoutException e) {
            status = "TIMEOUT";
        } catch (UnknownHostException e) {
            status = "DNS_FAIL";
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Status=")) {
                int start = msg.indexOf("Status=") + 7;
                int end = msg.indexOf(',', start);
                if (end == -1) end = msg.length();
                status = msg.substring(start, end).trim();
            } else if (msg != null && msg.contains("HTTP error")) {
                status = "HTTP_ERR";
            } else {
                status = "IO_ERR: " + (msg != null ? msg.substring(0, Math.min(40, msg.length())) : "unknown");
            }
        } catch (Exception e) {
            status = "EXCEPTION: " + e.getClass().getSimpleName();
        }

        String titleDisplay = title != null ? title.replace('\n', ' ').trim() : "";
        if (titleDisplay.length() > 40) {
            titleDisplay = titleDisplay.substring(0, 37) + "...";
        }
        String firstCharsDisplay = firstChars.replace('\n', ' ').replace('\r', ' ').trim();

        System.out.printf("%-50s | %-12s | %-16d | %-40s | %s%n",
                url, status, contentLength, titleDisplay, firstCharsDisplay);
        System.out.flush();
    }
}
