package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MassSiteScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(MassSiteScraper.class);
    private static final Random RANDOM = new Random();
    private static final int MAX_THREADS = 15;
    private static final int MAX_PAGES_PER_SITE = 5;

    private static final String[] BASE_URLS = {
        // === ANNUAIRE-GRATUIT.MA (by city) ===
        "https://www.annuaire-gratuit.ma/recherche/entreprise-ville-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/societe-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/annuaire-des-entreprises-ville-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/artisan-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/avocat-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/medecin-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/notaire-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/comptable-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/restaurant-casablanca.html",
        "https://www.annuaire-gratuit.ma/recherche/hotel-casablanca.html",

        // === TELECONTACT.MA ===
        "https://www.telecontact.ma/annuaire/casablanca/entreprises/",
        "https://www.telecontact.ma/annuaire/casablanca/artisans/",
        "https://www.telecontact.ma/annuaire/casablanca/professions-liberales/",
        "https://www.telecontact.ma/annuaire/casablanca/commerces/",
        "https://www.telecontact.ma/annuaire/casablanca/services/",
        "https://www.telecontact.ma/annuaire/casablanca/industrie/",
        "https://www.telecontact.ma/annuaire/casablanca/restaurants/",
        "https://www.telecontact.ma/annuaire/casablanca/hotels/",
        "https://www.telecontact.ma/annuaire/casablanca/banques/",
        "https://www.telecontact.ma/annuaire/casablanca/assurances/",
        "https://www.telecontact.ma/annuaire/casablanca/immobilier/",
        "https://www.telecontact.ma/annuaire/casablanca/transport/",
        "https://www.telecontact.ma/annuaire/casablanca/informatique/",
        "https://www.telecontact.ma/annuaire/casablanca/batiment/",
        "https://www.telecontact.ma/annuaire/casablanca/sante/",
        "https://www.telecontact.ma/annuaire/casablanca/education/",
        "https://www.telecontact.ma/recherche/casablanca/",

        // === PAGES-MAROC.COM ===
        "https://www.pages-maroc.com/annuaire/casablanca/",
        "https://www.pages-maroc.com/annuaire/casablanca/auto-moto/",
        "https://www.pages-maroc.com/annuaire/casablanca/banques-assurances/",
        "https://www.pages-maroc.com/annuaire/casablanca/batiment/",
        "https://www.pages-maroc.com/annuaire/casablanca/commerces/",
        "https://www.pages-maroc.com/annuaire/casablanca/communication/",
        "https://www.pages-maroc.com/annuaire/casablanca/education/",
        "https://www.pages-maroc.com/annuaire/casablanca/hotels-restaurants/",
        "https://www.pages-maroc.com/annuaire/casablanca/immobilier/",
        "https://www.pages-maroc.com/annuaire/casablanca/informatique/",
        "https://www.pages-maroc.com/annuaire/casablanca/sante/",
        "https://www.pages-maroc.com/annuaire/casablanca/services/",
        "https://www.pages-maroc.com/annuaire/casablanca/transport/",

        // === INFOCONTACT.MA ===
        "https://www.infocontact.ma/casablanca/entreprises",
        "https://www.infocontact.ma/casablanca/artisans",
        "https://www.infocontact.ma/casablanca/services",
        "https://www.infocontact.ma/casablanca/commerces",
        "https://www.infocontact.ma/casablanca/restaurants",
        "https://www.infocontact.ma/casablanca/sante",

        // === GO AFRICA ONLINE (Casablanca categories) ===
        "https://www.goafricaonline.com/ma/annuaire/casablanca",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/agences-de-communication",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/societes-informatiques",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/entreprises-batiment-construction",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/commerces",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/automobile-moto",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/immobilier",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/sante",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/education-formation",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/transport-logistique",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/tourisme-hotellerie",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/industrie",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/banques-assurances",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/restaurants",
        "https://www.goafricaonline.com/ma/annuaire/casablanca/hotels",

        // === CYLEX.MA Casablanca ===
        "https://www.cylex.ma/casablanca/",
        "https://www.cylex.ma/casablanca/restaurants.html",
        "https://www.cylex.ma/casablanca/banques.html",
        "https://www.cylex.ma/casablanca/immobilier.html",
        "https://www.cylex.ma/casablanca/sante.html",
        "https://www.cylex.ma/casablanca/automobile.html",
        "https://www.cylex.ma/casablanca/education.html",
        "https://www.cylex.ma/casablanca/informatique.html",
        "https://www.cylex.ma/casablanca/supermarches.html",

        // === HOTFROG.MA Casablanca ===
        "https://www.hotfrog.ma/casablanca/",
        "https://www.hotfrog.ma/casablanca/restaurants",
        "https://www.hotfrog.ma/casablanca/construction",
        "https://www.hotfrog.ma/casablanca/automobile",
        "https://www.hotfrog.ma/casablanca/sante",
        "https://www.hotfrog.ma/casablanca/education",
        "https://www.hotfrog.ma/casablanca/immobilier",
        "https://www.hotfrog.ma/casablanca/services",

        // === YALWA Casablanca ===
        // Skipped: classifieds, mostly individuals

        // === BISKOON Casablanca ===
        // Skipped: classifieds, mostly individuals

        // === MAROCANNONCES Casablanca ===
        // Skipped: classifieds, mostly individuals

        // === SOCIETE.MA ===
        "https://societe.ma/casablanca/",
        "https://societe.ma/casablanca/informatique",
        "https://societe.ma/casablanca/immobilier",
        "https://societe.ma/casablanca/restauration",
        "https://societe.ma/casablanca/transport",

        // === ENTREPRISES.MA ===
        "https://entreprises.ma/recherche/casablanca",
        "https://entreprises.ma/recherche/casablanca/batiment",
        "https://entreprises.ma/recherche/casablanca/transport",
        "https://entreprises.ma/recherche/casablanca/informatique",

        // === MAROC-ENTREPRISES ===
        "https://maroc-entreprises.com/annuaire/casablanca/",
        "https://maroc-entreprises.com/annuaire/casablanca/assurances",
        "https://maroc-entreprises.com/annuaire/casablanca/banques",
        "https://maroc-entreprises.com/annuaire/casablanca/immobilier",
        "https://maroc-entreprises.com/annuaire/casablanca/informatique",
        "https://maroc-entreprises.com/annuaire/casablanca/restauration",
        "https://maroc-entreprises.com/annuaire/casablanca/transport",
        "https://maroc-entreprises.com/annuaire/casablanca/batiment",
        "https://maroc-entreprises.com/annuaire/casablanca/sante",
        "https://maroc-entreprises.com/annuaire/casablanca/commerces",

        // === STE.MA ===
        "https://ste.ma/fr/search?q=casablanca",
        "https://ste.ma/fr/search?q=casablanca+informatique",
        "https://ste.ma/fr/search?q=casablanca+immobilier",
        "https://ste.ma/fr/search?q=casablanca+restaurant",
        "https://ste.ma/fr/search?q=casablanca+transport",
        "https://ste.ma/fr/search?q=casablanca+batiment",
        "https://ste.ma/fr/search?q=casablanca+sante",
        "https://ste.ma/fr/search?q=casablanca+assurance",
        "https://ste.ma/fr/search?q=casablanca+banque",
        "https://ste.ma/fr/search?q=casablanca+hotel",
        "https://ste.ma/fr/search?q=casablanca+avocat",
        "https://ste.ma/fr/search?q=casablanca+comptable",
        "https://ste.ma/fr/search?q=casablanca+notaire",
        "https://ste.ma/fr/search?q=casablanca+pharmacie",
        "https://ste.ma/fr/search?q=casablanca+coiffure",
        "https://ste.ma/fr/search?q=casablanca+restauration",
        "https://ste.ma/fr/search?q=casablanca+supermarche",
        "https://ste.ma/fr/search?q=casablanca+automobile",
        "https://ste.ma/fr/search?q=casablanca+electronique",
        "https://ste.ma/fr/search?q=casablanca+meuble",

        // === KOMPASS Casablanca ===
        "https://ma.kompass.com/businessplace/listing/casablanca",
        "https://ma.kompass.com/businessplace/listing/casablanca/services",
        "https://ma.kompass.com/businessplace/listing/casablanca/industrie",

        // === ADRESSES.MA ===
        "https://adresses.ma/annuaire/casablanca/",
        "https://adresses.ma/annuaire/casablanca/restaurants",
        "https://adresses.ma/annuaire/casablanca/hotels",
        "https://adresses.ma/annuaire/casablanca/batiment",
        "https://adresses.ma/annuaire/casablanca/sante",
        "https://adresses.ma/annuaire/casablanca/education",
        "https://adresses.ma/annuaire/casablanca/transport",
        "https://adresses.ma/annuaire/casablanca/commerces",

        // === MONNOTAIRE / AVOCAT Casablanca ===
        "https://www.monnotaire.ma/annuaire/avocat-casablanca",
        "https://www.monnotaire.ma/annuaire/notaire-casablanca",
        "https://www.monnotaire.ma/annuaire/architecte-casablanca",
        "https://www.monnotaire.ma/annuaire/comptable-casablanca",
        "https://www.monnotaire.ma/annuaire/medecin-casablanca",
        "https://www.monnotaire.ma/annuaire/dentiste-casablanca",
        "https://www.monnotaire.ma/annuaire/pharmacie-casablanca",
        "https://www.monnotaire.ma/annuaire/avocat-casablanca/page2",
        "https://www.monnotaire.ma/annuaire/notaire-casablanca/page2",

        // === YELLOWPAGES.MA ===
        "https://www.yellowpages.ma/places/casablanca",
        "https://www.yellowpages.ma/places/casablanca/banques",
        "https://www.yellowpages.ma/places/casablanca/restaurants",
        "https://www.yellowpages.ma/places/casablanca/hotels",
        "https://www.yellowpages.ma/places/casablanca/hopitaux",

        // === SOCIETES-AU-MAROC ===
        "https://societes-au-maroc.com/",
        "https://societes-au-maroc.com/casablanca/",
        "https://societes-au-maroc.com/casablanca/immobilier/",
        "https://societes-au-maroc.com/casablanca/restauration/",
        "https://societes-au-maroc.com/casablanca/transport/",
        "https://societes-au-maroc.com/casablanca/informatique/",
        "https://societes-au-maroc.com/casablanca/batiment/",

        // === MOROCCOBUSINESS ===
        "https://moroccan.biz/directory/casablanca",
        "https://moroccan.biz/directory/casablanca/services",
        "https://moroccan.biz/directory/casablanca/shops",

        // === AFRIKTA Morocco ===
        "https://afrikta.com/listing-locations/morocco/",
        "https://afrikta.com/listing-locations/morocco/page/2/",
        "https://afrikta.com/listing-locations/morocco/page/3/",

        // === EARABICMARKET ===
        "https://www.earabicmarket.com/en/companies/morocco/all",
        "https://www.earabicmarket.com/en/companies/morocco/casablanca",

        // === EUROPAGES ===
        "https://www.europages.com/companies/Morocco.html",
        "https://www.europages.com/companies/Morocco/Construction.html",
        "https://www.europages.com/companies/Morocco/Services.html",
        "https://www.europages.com/companies/Morocco/IT.html",
        "https://www.europages.com/companies/Morocco/Finance.html",
        "https://www.europages.com/companies/Morocco/Transport.html",

        // === JOB SITES Casablanca (offers only, no CVs) ===
        "https://www.emploi.ma/offres-emploi-casablanca",
        "https://www.emploi.ma/offres-emploi-casablanca/informatique",
        "https://www.emploi.ma/offres-emploi-casablanca/commercial",
        "https://www.emploi.ma/offres-emploi-casablanca/finance",
        "https://www.emploi.ma/offres-emploi-casablanca/ingenierie",

        // === NEWS SITES - Mentioning Casablanca companies ===
        "https://www.lematin.ma/entreprise/",
        "https://aujourdhui.ma/economie/",
        "https://leseco.ma/",
        "https://www.challenge.ma/",
        "https://www.madein-maroc.net/",
        "https://maroc-diplomatique.net/category/economie/",

        // === SECTEUR SPECIFIQUE ===
        "https://www.el-annuaire.com/annuaire-maroc/casablanca",
        "https://www.izifree.com/annuaire-entreprises/casablanca",
        "https://www.le-guide.info/annuaire-maroc/casablanca",
        "https://www.marocindex.com/annuaire/casablanca",
        "https://www.marocannuaire.com/",
        "https://www.marocannuaire.com/casablanca",
        "https://www.guide-annuaire-maroc.com/annuaire/casablanca",
        "https://www.123casa.ma/",
        "https://www.casablancacity.ma/",
        "https://www.casanet.ma/annuaire/",
        "https://www.ramadan.ma/annuaire/casablanca",
        "https://www.lespagesjaunes.ma/annuaire/casablanca",
        "https://www.trouver.ma/annuaire/casablanca",
        "https://www.opticien.ma/annuaire/casablanca",
        "https://www.photo.ma/annuaire/casablanca",
        "https://www.architecture.ma/annuaire/casablanca",
        "https://www.assurance.ma/annuaire/casablanca",
        "https://www.banque.ma/annuaire/casablanca",
    };

    private static String[] generateCategoryUrls() {
        String[] categories = {
            "immobilier", "informatique", "restaurant", "transport", "batiment",
            "sante", "assurance", "banque", "hotel", "avocat", "comptable",
            "notaire", "pharmacie", "coiffure", "restauration", "supermarche",
            "automobile", "electronique", "meuble", "vetements", "bijouterie",
            "fleuriste", "boulangerie", "boucherie", "coiffeur", "esthetique",
            "sport", "fitness", "gym", "ecole", "universite", "formation",
            "cabinet", "conseil", "audit", "ingenierie", "architecture",
            "plomberie", "electricite", "peinture", "menuisier", "serrurier",
            "jardinier", "nettoyage", "securite", "gardiennage", "livraison",
            "voyage", "tourisme", "location", "vente", "import", "export",
            "industrie", "agriculture", "alimentaire", "textile", "bois",
            "metallurgie", "chimie", "plastique", "emballage", "logistique",
            "communication", "marketing", "publicite", "photographie",
            "evenementiel", "traiteur", "cafe", "patisserie", "glacier",
            "quincaillerie", "droguerie", "parapharmacie", "opticien",
            "dentiste", "clinique", "laboratoire", "radiologie",
            "hopital", "ambulance", "creche", "jardin", "piscine",
        };
        String[] result = new String[categories.length * 4];
        int i = 0;
        for (String cat : categories) {
            result[i++] = "https://www.annuaire-gratuit.ma/recherche/" + cat + "-casablanca.html";
            result[i++] = "https://ste.ma/fr/search?q=casablanca+" + cat;
            result[i++] = "https://www.telecontact.ma/recherche/" + cat + "+casablanca";
            result[i++] = "https://www.cylex.ma/casablanca/" + cat + ".html";
        }
        return result;
    }

    public MassSiteScraper() {
        super("Mass Sites", "SITE_DIRECT");
    }

    @Override
    public List<CeoContact> scrape() {
        Set<CeoContact> allContacts = ConcurrentHashMap.newKeySet();
        String[] categoryUrls = generateCategoryUrls();

        List<String> allUrls = new ArrayList<>();
        allUrls.addAll(Arrays.asList(BASE_URLS));
        allUrls.addAll(Arrays.asList(categoryUrls));

        log.info("MassSite: {} URLs with {} threads, up to {} pages each",
            allUrls.size(), MAX_THREADS, MAX_PAGES_PER_SITE);

        AtomicInteger totalContacts = new AtomicInteger(0);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (String url : allUrls) {
            futures.add(executor.submit(() -> {
                try {
                    scrapeUrlWithPagination(url, allContacts, totalContacts);
                } catch (Exception ignored) {}
            }));
        }

        // Wait for all to complete
        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();
        try { executor.awaitTermination(10, TimeUnit.SECONDS); } catch (Exception ignored) {}

        List<CeoContact> result = new ArrayList<>(allContacts);
        log.info("MassSite total: {} contacts ({} unique)", totalContacts.get(), result.size());
        return result;
    }

    private void scrapeUrlWithPagination(String url, Set<CeoContact> allContacts, AtomicInteger totalContacts) {
        try {
            String baseUrl = url;
            String pageParam = "?page=";
            if (url.contains("?")) pageParam = "&page=";
            String pageParam2 = "/page/";

            for (int page = 1; page <= MAX_PAGES_PER_SITE; page++) {
                String pageUrl = baseUrl;
                if (page > 1) {
                    if (baseUrl.contains("annuaire-gratuit") || baseUrl.contains("ste.ma") || baseUrl.contains("infocontact")) {
                        pageUrl = baseUrl + pageParam + (page - 1);
                    } else if (baseUrl.contains("telecontact")) {
                        pageUrl = baseUrl + pageParam2 + page + "/";
                    } else {
                        pageUrl = baseUrl + pageParam + page;
                    }
                }

                List<CeoContact> contacts = scrapeSingleUrl(pageUrl);
                if (contacts.isEmpty()) break;

                for (CeoContact c : contacts) {
                    if (allContacts.add(c)) {
                        totalContacts.incrementAndGet();
                    }
                }

                sleep(100, 300);
            }
        } catch (Exception ignored) {}
    }

    private List<CeoContact> scrapeSingleUrl(String url) {
        List<CeoContact> results = new ArrayList<>();
        try {
            String html = Jsoup.connect(url)
                .userAgent(randomUA())
                .timeout(10000)
                .followRedirects(true)
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .execute().body();

            if (html == null || html.length() < 200) return results;

            List<PageParser.ScrapedEntry> entries = PageParser.extractEntries(html, url, "Casablanca");
            for (PageParser.ScrapedEntry entry : entries) {
                if (entry.phoneNumber == null) continue;
                CeoContact contact = new CeoContact();
                contact.setCompanyName(entry.companyName);
                contact.setPhoneNumber(entry.phoneNumber);
                contact.setSourceUrl(url);
                contact.setSourceType(getSourceType());
                contact.setLastVerifiedAt(LocalDateTime.now());
                results.add(contact);
            }
        } catch (Exception ignored) {}
        return results;
    }

    private String randomUA() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36";
    }

    private void sleep(int min, int max) {
        try { Thread.sleep(min + RANDOM.nextInt(max - min)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
