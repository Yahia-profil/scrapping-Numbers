package com.ceotracker.service.scraper;

import com.ceotracker.service.PhoneNormalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser {

    private static final Pattern PHONE_PERSONAL = Pattern.compile(
        "(?:0[67]|\\+212\\s*[67]|00212\\s*[67])\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d");

    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
        "(?i)(SARL|SA|SAS|SCA|EURL|SNC|SPA|SRL|GIE|EARL|" +
        "INDUSTRIE|SERVICES|TRADING|IMPORT|EXPORT|TRANSPORT|BATIMENT|" +
        "IMMOBILIER|ASSURANCE|BANQUE|FINANCE|CONSTRUCTION|COMMUNICATION|" +
        "INFORMATIQUE|ENERGIE|AGRICOLE|ALIMENTAIRE|DISTRIBUTION|" +
        "INGENIERIE|MANAGEMENT|CONSEIL|AUDIT|COMPTABILITE|LOGISTIQUE|" +
        "TRAVAUX|ELECTRICITE|PLOMBERIE|PEINTURE|DECORATION|NETTOYAGE|" +
        "SECURITE|GARDIENNAGE|MAINTENANCE|ENTRETIEN|RESTAURATION|" +
        "HOTEL|TOURISME|VOYAGES|LIVRAISON|AUTOMOBILE|" +
        "MAROC|MOROCCO|CASABLANCA|RABAT|TANGER|MARRAKECH)");

    private static final Pattern COMPANY_KEYWORD = Pattern.compile(
        "(?i)(SOCIETE|ENTREPRISE|COMPAGNIE|GROUPE|HOLDING|" +
        "CABINET|STUDIO|CENTRE|CLINIQUE|LABORATOIRE|PHARMACIE|" +
        "ECOLE|INSTITUT|UNIVERSITE|COLLEGE|" +
        "HOTEL|RESTAURANT|SUPERMARCHE|MARCHE|" +
        "GARAGE|ATELIER|USINE|FACTORY|" +
        "COIFFURE|INSTITUT\\s+DE\\s+BEAUTE|" +
        "AGENCE|BUREAU|OFFICE|" +
        "FONDATION|ASSOCIATION|ORGANISME|" +
        "SOCIETE\\s+D'|ENTREPRISE\\s+D')");

    private static final Pattern INDIVIDUAL_NAME = Pattern.compile(
        "(?i)^(Particulier|Particuliers|Vendeur|Acheteur|" +
        "Bonjour|Annonce|Urgent|Vends|Cherche|Recherche|" +
        "Monsieur|Madame|Mme|Mlle|Mr|Dr|Professeur|" +
        "Contactez|Appelez|Tel|Téléphone|Portable|" +
        "Disponible|Libre|Proposé|Offre|Demande|" +
        "A\\s+vendre|A\\s+louer|Vente|Location|" +
        "Cv|Curriculum|Candidat|Candidature|" +
        "Maison|Appartement|Villa|Terrain|Bureau|Local|" +
        "Chambre|Studio|Duplex|" +
        "\\d{5,}.*)$");

    public static List<ScrapedEntry> extractEntries(String html, String url) {
        return extractEntries(html, url, null);
    }

    public static List<ScrapedEntry> extractEntries(String html, String url, String cityFilter) {
        Set<ScrapedEntry> entries = new LinkedHashSet<>();
        Document doc = Jsoup.parse(html);
        String text = doc.body().text();
        String normalized = text.replaceAll("[\\s\\-–—.()/]+", "");

        // City filter: check entire page text once
        boolean pageHasCity = true;
        if (cityFilter != null && !cityFilter.isEmpty()) {
            String lowerText = text.toLowerCase();
            pageHasCity = lowerText.contains(cityFilter.toLowerCase())
                || lowerText.contains("casa")
                || lowerText.contains(cityFilter.toLowerCase() + "naise")
                || lowerText.contains(cityFilter.toLowerCase() + "nais")
                || url.toLowerCase().contains(cityFilter.toLowerCase());
        }

        // Find personal numbers (06/07)
        Matcher personalMatcher = PHONE_PERSONAL.matcher(normalized);
        while (personalMatcher.find()) {
            String rawDigits = personalMatcher.group().replaceAll("\\s+", "");
            String phone = PhoneNormalizer.normalize(rawDigits);
            if (phone == null || !phone.matches("0[67]\\d{8}")) continue;

            if (!pageHasCity) continue;

            int origStart = findOriginalPosition(text, normalized, personalMatcher.start());
            int start = Math.max(0, origStart - 300);
            int end = Math.min(text.length(), origStart + 200);
            String context = text.substring(start, end);

            ScrapedEntry entry = new ScrapedEntry();
            entry.phoneNumber = phone;
            entry.companyName = findCompany(doc, context, origStart);
            entry.sourceUrl = url;

            boolean dup = false;
            for (ScrapedEntry e : entries) {
                if (e.phoneNumber.equals(phone)) { dup = true; break; }
            }
            if (!dup) entries.add(entry);
        }

        return new ArrayList<>(entries);
    }

    private static int findOriginalPosition(String text, String normalized, int posInNormalized) {
        int ti = 0, ni = 0;
        while (ni < posInNormalized && ti < text.length()) {
            if (!Character.isWhitespace(text.charAt(ti)) && text.charAt(ti) != '-'
                && text.charAt(ti) != '–' && text.charAt(ti) != '.' && text.charAt(ti) != '('
                && text.charAt(ti) != ')' && text.charAt(ti) != '/') {
                if (ni >= posInNormalized) break;
                ni++;
            }
            ti++;
        }
        return Math.min(ti, text.length() - 1);
    }

    private static String findCompany(Document doc, String context, int aroundPos) {
        // Method 1: Find the element containing the phone number and walk up
        Element phoneContainer = findPhoneContainer(doc, aroundPos);
        if (phoneContainer != null) {
            for (Element link : phoneContainer.select("a, strong, b, h1, h2, h3, h4, .company, .nom, .titre, [class*=company], [class*=societe], [class*=nom]")) {
                String name = link.text().trim();
                if (name.length() > 3 && name.length() < 120 && !name.matches(".*\\d{6,}.*")) {
                    name = name.replaceAll("(?i)^(entreprise\\s+|soci[eé]t[eé]\\s+|compagnie\\s+)+", "");
                    if (isValidCompany(name)) return name;
                }
            }
            String containerText = phoneContainer.text().trim();
            String[] lines = containerText.split("[\n\r]+");
            for (String line : lines) {
                line = line.trim().replaceAll("^[•·\\-*]+", "");
                if (line.length() > 5 && line.length() < 120 && isValidCompany(line)) {
                    return line;
                }
            }
        }

        // Method 2: Look for company-specific CSS classes
        for (Element el : doc.select("[class*=company], [class*=societe], [class*=raison], [class*=enseigne], " +
            "[class*=nom-entreprise], [itemprop=name], .company-name, .societe-name, .business-name")) {
            String name = el.text().trim();
            if (name.length() > 3 && isValidCompany(name)) return name;
        }

        // Method 3: Check context for company legal form suffix (SARL, SA, etc.)
        Matcher suffixM = COMPANY_SUFFIX.matcher(context);
        if (suffixM.find()) {
            int end = suffixM.start();
            int start = Math.max(0, end - 80);
            String before = context.substring(start, end).trim();
            String[] words = before.split("[\\s,;:]+");
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, words.length - 3); i < words.length; i++) {
                if (words[i].length() > 1 && Character.isUpperCase(words[i].charAt(0))) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(words[i]);
                }
            }
            if (sb.length() > 0) {
                String name = sb.toString() + " " + suffixM.group();
                if (isValidCompany(name)) return name;
            }
        }

        // Method 4: Line-by-line scan of context
        String[] lines = context.split("[\n\r]+");
        for (String line : lines) {
            line = line.trim().replaceAll("^[\\s•·\\-*]+", "");
            if (line.length() > 5 && line.length() < 120 && isValidCompany(line)) {
                return line;
            }
        }

        // Method 5: Try nearest heading
        Element nearest = findNearestHeading(doc, aroundPos);
        if (nearest != null) {
            String hText = nearest.text().trim();
            if (isValidCompany(hText)) return hText;
        }

        return "Inconnue";
    }

    private static Element findPhoneContainer(Document doc, int textPos) {
        String bodyText = doc.body().text();
        if (textPos < 0 || textPos >= bodyText.length()) return null;

        String[] containers = {"tr", "li", "div.result", "div.item", "div.entry", "article", "section",
            ".card", ".panel", ".listing-item", ".business-card", ".contact-card"};
        for (String sel : containers) {
            for (Element el : doc.select(sel)) {
                String elText = el.text();
                int idx = bodyText.indexOf(elText);
                if (idx >= 0 && Math.abs(idx - textPos) < 500) {
                    return el;
                }
            }
        }
        return null;
    }

    private static final Set<String> CITY_NAMES = Set.of(
        "casablanca", "casa", "rabat", "marrakech", "tanger", "fes", "meknes",
        "oujda", "kenitra", "agadir", "safi", "el jadida", "tetouan", "settat",
        "beni mellal", "laayoune", "dakhla", "essaouira", "taza", "mohammedia",
        "khouribga", "berrechid", "had soualem", "berkane", "sale", "temara",
        "ain sebaa", "derb sultan", "ben souda", "hay mohammadi",
        "bouskoura", "dar bouazza", "médiouna", "nouasseur",
        "ain chock", "anfa", "maarif", "californie", "bourgone",
        "almenssour", "beauté", "diour jamaa", "beni makada");

    private static final Pattern INDIVIDUAL_PATTERN = Pattern.compile(
        "(?i)^(?:M[^a-z]{0,2}\\s+|Monsieur\\s+|Madame\\s+|Mme\\s+|Mlle\\s+|" +
        "Dr\\s+|Professeur\\s+|Mr\\s+|Ms\\s+)" +
        "[A-Z][A-Za-zéèêëàâùûüôöîïç]{2,}\\s+[A-Z][A-Za-zéèêëàâùûüôöîïç]{2,}");

    private static final Pattern CITY_PATTERN = Pattern.compile(
        "^\\d{4,5}\\s+[A-Z]{2,}$");

    private static final Pattern JOB_TITLE = Pattern.compile(
        "(?i)(directeur\\s+g[eé]n[eé]ral|responsable|manager|g[eé]rant|pr[eé]sident|" +
        "chef\\s+de|superviseur|coordinateur|contr[oô]leur|assistant\\s+de|" +
        "charg[eé]\\s+de|consultant|expert|comptable|ing[eé]nieur|technicien|" +
        "contrema[îi]tre|repr[eé]sentant|d[eé]l[eé]gu[eé]|conseiller|" +
        "fondateur|dir\\.\\s*g[eé]n|dir\\s+g[eé]n|g[eé]rant)");

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(?i)(?:^|,\\s*)" +
        "\\d+[\\s,]*\\s*(?:rue|bd|boulevard|avenue|av|route|rte|lot|ilot|place|impasse|" +
        "square|resid|residence|r[eé]s|b[âa]t|bat|imm|immeuble|zone|z\\.i|angle|" +
        "km|bp|b\\.p|n°|n�|n\\.°|appart|appt)\\s.*");

    private static final Pattern ADDRESS_FRAGMENT = Pattern.compile(
        "(?i)(?:\\d+[\\s,]*)?(?:rue|bd|boulevard|avenue|route|rte|lot|ilot|place|" +
        "zone\\s+franche|angle|km|immeuble|r[eé]sidence|r[eé]s\\s)" +
        "[\\s\\w,'\\.-]+");

    private static final Pattern LOCATION_DESC = Pattern.compile(
        "(?i)^(?:rte\\s+de|route\\s+de|avenue\\s+|bd\\s+|rue\\s+" +
        "|quartier\\s+|lotissement\\s+|r[eé]sidence\\s+)" +
        "[a-z].*");

    private static final Pattern GENERIC_CATEGORY = Pattern.compile(
        "(?i)^(?:services|automobile|parachimie|confection|industrie|textile|" +
        "alimentation|transport|batiment|immobilier|informatique|communication|" +
        "construction|import|export|distribution|production|fabrication|" +
        "maintenance|nettoyage|s[eé]curit[eé]|restauration|tourisme|" +
        "h[oô]tellerie|assurance|banque|finance|conseil|audit|formation|" +
        "location|vente|achat|commerce|g['eé]n['eé]ral[e]?)$");

    public static boolean isValidCompany(String s) {
        if (s == null || s.isBlank()) return false;
        String cleaned = s.replaceAll("\\s+", " ").trim();
        String lower = cleaned.toLowerCase();

        // Length: minimum 6, maximum 100
        if (cleaned.length() < 6 || cleaned.length() > 100) return false;

        // Must start with an uppercase letter or digit
        char first = cleaned.charAt(0);
        if (!Character.isUpperCase(first) && !Character.isDigit(first)) return false;

        // Contains a long number sequence (phone or large code)
        if (cleaned.matches(".*\\d{6,}.*")) return false;

        // Contains encoding issues
        if (cleaned.contains("?") || cleaned.contains("�")) return false;

        // Reject UI/navigation garbage
        if (lower.contains("téléphone") || lower.contains("telephone") || lower.contains("email")
            || lower.contains("adresse") || lower.contains("fax") || lower.contains("contact")
            || lower.startsWith("www") || lower.startsWith("http") || lower.contains("horaires")
            || lower.contains("facebook") || lower.contains("instagram") || lower.contains("linkedin")
            || lower.contains("twitter") || lower.contains("youtube") || lower.contains("suivez")
            || lower.contains("partager") || lower.contains("copyright") || lower.contains("tous droits")
            || lower.contains("mentions") || lower.contains("plan du site") || lower.contains("connexion")
            || lower.contains("inscription") || lower.contains("mot de passe") || lower.contains("envoyer")
            || lower.contains("newsletter") || lower.contains("recherche") || lower.contains("résultat")
            || lower.contains("navigation") || lower.contains("accueil") || lower.contains("retour")
            || lower.contains("cliquez") || lower.contains("précédent") || lower.contains("suivant")
            || lower.contains("commenter") || lower.contains("abonnez") || lower.contains("charger")
            || lower.contains("afficher") || lower.contains("trier") || lower.contains("filtrer")
            || lower.contains("choisir") || lower.contains("secteur d") || lower.contains("confection")
            || lower.contains("@") || lower.contains("gsm") || lower.contains("mobile")
            || lower.contains("portable"))
            return false;

        // Reject individual/private listings
        if (INDIVIDUAL_NAME.matcher(cleaned).find()) return false;
        if (INDIVIDUAL_PATTERN.matcher(cleaned).find()) return false;

        // Reject job titles
        if (JOB_TITLE.matcher(cleaned).find()) return false;

        // Reject single-word entries
        String[] words = cleaned.split("\\s+");
        if (words.length < 2) return false;

        // Reject 2-word generic categories
        if (words.length <= 2 && GENERIC_CATEGORY.matcher(lower).find()) return false;

        // Reject if starts with a number followed by space (address pattern)
        if (words[0].matches("^\\d+$") && words.length <= 6) return false;

        // Reject address patterns (number + street type)
        if (ADDRESS_PATTERN.matcher(lower).find()) return false;
        if (LOCATION_DESC.matcher(lower).find()) return false;

        // Reject if it's just a city name or postal code + city
        if (CITY_NAMES.contains(lower.trim())) return false;
        if (CITY_PATTERN.matcher(cleaned).find()) return false;

        // Reject if 3+ words but all are cities/location descriptors
        int cityWords = 0;
        for (String w : words) {
            if (CITY_NAMES.contains(w.toLowerCase())) cityWords++;
        }
        if (cityWords >= words.length / 2) return false;

        // Reject individual name patterns (First LASTNAME or F. LASTNAME)
        if (words.length <= 3) {
            if (lower.matches("^[a-zéèêëàâùûüôöîïç']+\\s+[a-zéèêëàâùûüôöîïç']+$")) {
                if (words.length == 2 && Character.isUpperCase(words[0].charAt(0))
                    && Character.isUpperCase(words[1].charAt(0))
                    && words[0].length() >= 3 && words[1].length() >= 3) {
                    return false;
                }
            }
            // Reject "Name TITLE" patterns
            if (lower.matches("^[a-zéèêëàâùûüôöîïç]{3,}\\s+\\(.*\\)$")) return false;
        }

        // Strong indicator: has legal form (SARL, SA, etc.)
        if (COMPANY_SUFFIX.matcher(cleaned).find()) return true;

        // Strong indicator: has company keyword (Société, Cabinet, etc.)
        if (COMPANY_KEYWORD.matcher(cleaned).find()) return true;

        // Count capitalized words and real words
        int upper = 0;
        int realWords = 0;
        for (String w : words) {
            if (w.length() > 1 && Character.isLetter(w.charAt(0))) realWords++;
            if (w.length() > 0 && Character.isUpperCase(w.charAt(0))) upper++;
        }
        if (realWords < 2) return false;

        // Must have at least 3 uppercase words, or 2/3 of words must be uppercase
        if (upper >= Math.max(3, words.length * 2 / 3)) return true;

        // Also accept if most words are capitalized AND >= 3 words
        if (words.length >= 3 && upper >= words.length - 1) return true;

        // Accept 2-word names if BOTH are long (>=5 chars) capitalized words with company suffix nearby
        if (words.length == 2 && upper == 2 && words[0].length() >= 5 && words[1].length() >= 5
            && (COMPANY_SUFFIX.matcher(lower).find() || COMPANY_KEYWORD.matcher(lower).find())) return true;

        return false;
    }

    private static Element findNearestHeading(Document doc, int pos) {
        Element best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Element h : doc.select("h1, h2, h3, h4, h5, .company-name, .titre, .title, .nom, .raison-sociale, [class*=title], [class*=nom], [class*=company], [class*=societe]")) {
            try {
                String text = h.text();
                if (text.isEmpty() || text.length() < 3) continue;
                int hPos = doc.body().text().indexOf(text);
                if (hPos < 0) continue;
                int dist = Math.abs(hPos - pos);
                if (dist < bestDist && dist < 800) {
                    bestDist = dist;
                    best = h;
                }
            } catch (Exception ignored) {}
        }
        return best;
    }

    public static List<String> extractLinks(String html, String baseUri) {
        List<String> links = new ArrayList<>();
        Document doc = Jsoup.parse(html, baseUri);
        for (Element a : doc.select("a[href]")) {
            try {
                String href = a.absUrl("href");
                if (href.startsWith("http") && !href.contains("facebook")
                    && !href.contains("twitter") && !href.contains("linkedin")) {
                    links.add(href);
                }
            } catch (Exception ignored) {}
        }
        return links;
    }

    public static class ScrapedEntry {
        public String phoneNumber;
        public String companyName;
        public String sourceUrl;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof ScrapedEntry e) {
                return Objects.equals(phoneNumber, e.phoneNumber);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(phoneNumber);
        }
    }
}
