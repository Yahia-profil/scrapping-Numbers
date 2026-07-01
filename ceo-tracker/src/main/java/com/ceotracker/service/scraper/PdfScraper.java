package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.ActivityNormalizer;
import com.ceotracker.service.PhoneNormalizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PdfScraper {

    private static final Logger log = LoggerFactory.getLogger(PdfScraper.class);
    private static final String PDF_DIR = "C:\\Users\\user\\Desktop\\scrap";
    private static final String CLASSPATH_PDF = "/RH Emails - The bigest data base-1.pdf";

    private final String name;
    private final String sourceType;

    private static final Pattern GSM_ANY = Pattern.compile("0[67]\\s*(?:\\d\\s*){8}");
    private static final Pattern EMAIL_PAT = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CITY_PAT;
    private static final Pattern PERSON_PREFIX = Pattern.compile("(M\\.|Mme|Mlle|Mr)\\s+");

    private static final Set<String> CITIES = Set.of(
        "CASABLANCA", "RABAT", "MARRAKECH", "TANGER", "FES", "MEKNES",
        "OUJDA", "KENITRA", "AGADIR", "SAFI", "EL JADIDA", "TETOUAN",
        "SETTAT", "BENI MELLAL", "LAAYOUNE", "DAKHLA", "ESSAOUIRA",
        "TAZA", "MOHAMMEDIA", "KHOURIBGA", "BERKANE", "SALE", "TEMARA",
        "SIDI KACEM", "LARACHE", "TATA", "TIZNIT", "TAROUDANT",
        "BOUSKOURA", "AIN HARROUDA", "SKHIRAT", "BIR JDID", "BERRECHID",
        "AKKA", "IFRAN", "VALENCIA", "HAD SOUALEM", "MEDIOUNA",
        "NOUASSEUR", "DAR BOUAZZA", "AIT MELLOUL"
    );

    private static final Set<String> TITLE_KEYWORDS = new HashSet<>(Arrays.asList(
        "DIRECTEUR", "DIRECTRICE", "RESPONSABLE", "CHEF", "MANAGER", "GERANT",
        "G\u00c9RANT", "PRESIDENT", "PR\u00c9SIDENT", "ATTACHE", "ATTACH\u00c9",
        "ASSISTANT", "ASSISTANTE", "COORDINATEUR", "COORDINATRICE",
        "INGENIEUR", "ING\u00c9NIEUR", "TECHNICIEN", "TECHNICIENNE",
        "COMPTABLE", "CADRE", "CONTROLEUR", "CONTR\u00d4LEUR",
        "SENIOR", "ANALYST", "PURCHASING", "OFFICER",
        "SERVICE", "DEPARTEMENT", "D\u00c9PARTEMENT", "ADMINISTRATIF",
        "FINANCIER", "COMMERCIAL", "COMMERCIALE", "ACHATS", "IMPORT",
        "MAINTENANCE", "PRODUCTION", "LOGISTIQUE", "QUALITE", "QUALIT\u00c9",
        "SECURITE", "S\u00c9CURIT\u00c9", "ENVIRONNEMENT", "MOYENS",
        "DEVELOPPEMENT", "D\u00c9VELOPPEMENT", "HYDRAULIQUE",
        "GENERAL", "G\u00c9N\u00c9RAL", "TECHNIQUE", "TECHNOLOGIQUE",
        "INDUSTRIEL", "INDUSTRIELLE",
        "SUPERVISEUR", "FORMATEUR", "FORMATRICE", "EXPERT",
        "OFFICIER", "CONTREMAITRE", "CONTREMA\u00ceTRE",
        "DELEGUE", "D\u00c9L\u00c9GU\u00c9", "CONSEILLER",
        "FONDATEUR", "ASSOCIE", "ASSOCI\u00c9", "PDG", "P.D.G",
        "DRH", "QSE", "RH", "SQE", "MTN", "OHSE",
        "CHARGEE", "CHARG\u00c9E", "CHARGE", "CHARG\u00c9",
        "PROCUREMENT", "ADMINISTRATEUR",
        "PROSPECTION", "PUBLICATION", "MONETIQUE", "MON\u00c9TIQUE",
        "APPROVISIONNEMENT", "APPRO", "DIR",
        "ANNEX", "LOCBP", "LOBP"
    ));

    static {
        String joined = String.join("|", CITIES);
        CITY_PAT = Pattern.compile("\\b(" + joined + ")\\b");
    }

    public PdfScraper() {
        this.name = "PDF RH Emails";
        this.sourceType = "PDF";
    }

    public String getName() { return name; }
    public String getSourceType() { return sourceType; }

    public List<CeoContact> scrape() {
        Set<CeoContact> results = new LinkedHashSet<>();

        // Try classpath first (for Docker/deployment)
        try (InputStream is = getClass().getResourceAsStream(CLASSPATH_PDF)) {
            if (is != null) {
                File temp = File.createTempFile("rh_emails", ".pdf");
                try {
                    Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    List<CeoContact> contacts = extractFromPdf(temp);
                    results.addAll(contacts);
                    log.info("PDF classpath: {} contacts", contacts.size());
                } finally {
                    temp.delete();
                }
            }
        } catch (Exception e) {
            log.debug("PDF classpath non trouve: {}", e.getMessage());
        }

        // Fallback: filesystem (local dev)
        if (results.isEmpty()) {
            try (Stream<Path> paths = Files.list(Paths.get(PDF_DIR))) {
                List<Path> pdfFiles = paths
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .toList();
                for (Path pdfPath : pdfFiles) {
                    try {
                        List<CeoContact> contacts = extractFromPdf(pdfPath.toFile());
                        results.addAll(contacts);
                        log.info("PDF [{}]: {} contacts", pdfPath.getFileName(), contacts.size());
                    } catch (Exception e) {
                        log.debug("PDF erreur {}: {}", pdfPath.getFileName(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("PDF: erreur listing: {}", e.getMessage());
            }
        }

        List<CeoContact> result = new ArrayList<>(results);
        log.info("PDF total: {} contacts", result.size());
        return result;
    }

    private List<CeoContact> extractFromPdf(File file) {
        List<CeoContact> results = new ArrayList<>();
        Set<String> seenPhones = new HashSet<>();

        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            String[] rawLines = text.split("[\n\r]+");

            List<String> merged = mergeLines(rawLines);

            for (String line : merged) {
                try {
                    String city = extractCity(line);
                    String gsm = extractGSM(line);
                    if (gsm == null) continue;

                    // Skip if all numbers already seen
                    String[] numbers = gsm.split(" / ");
                    boolean allSeen = true;
                    for (String n : numbers) {
                        if (!seenPhones.contains(n)) { allSeen = false; break; }
                    }
                    if (allSeen) continue;
                    for (String n : numbers) { seenPhones.add(n); }

                    String person = extractPerson(line);
                    // Some contacts are incomplete in the PDF, that's normal
                    String company = extractCompany(line, city);
                    String fonction = extractFonction(line, person);
                    String rawActivity = extractActivity(line, city, person);

                    CeoContact contact = new CeoContact();
                    contact.setCompanyName(company != null && company.length() <= 100 ? company : "Inconnue");
                    contact.setCeoName(person);
                    contact.setPhoneNumber(gsm);
                    contact.setJobTitle(fonction);
                    contact.setActivity(rawActivity);
                    contact.setActivityCategory(ActivityNormalizer.normalize(rawActivity));
                    contact.setCity(city);
                    contact.setSourceType(getSourceType());
                    contact.setLastVerifiedAt(LocalDateTime.now());
                    results.add(contact);
                } catch (Exception e) {
                    log.debug("Erreur parsing ligne: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Erreur extraction PDF {}: {}", file.getName(), e.getMessage());
        }
        return results;
    }

    private List<String> mergeLines(String[] rawLines) {
        List<String> merged = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String raw : rawLines) {
            String l = raw.trim();
            if (l.isEmpty() || l.startsWith("SOCIETE")) continue;
            if (cur.isEmpty()) {
                cur.append(l);
            } else if (l.matches("^[A-Z0-9].*") && !l.startsWith("M.") && !l.startsWith("Mme") && !l.startsWith("Mlle") && !l.contains("@") && !l.matches("^0[567].*")) {
                merged.add(cur.toString());
                cur = new StringBuilder(l);
            } else {
                cur.append(" ").append(l);
            }
        }
        if (!cur.isEmpty()) merged.add(cur.toString());
        return merged;
    }

    private String extractGSM(String line) {
        String clean = line.replaceAll("0[5]\\s*(?:\\d\\s*){8}", "");
        Matcher emailM = EMAIL_PAT.matcher(clean);
        if (emailM.find()) {
            clean = clean.substring(0, emailM.start());
        }
        Matcher m = GSM_ANY.matcher(clean);
        Set<String> gsms = new LinkedHashSet<>();
        while (m.find()) {
            String raw = m.group().replaceAll("\\s+", "");
            String norm = PhoneNormalizer.normalize(raw);
            if (norm != null && norm.matches("0[67]\\d{8}")) {
                gsms.add(norm);
            }
        }
        if (gsms.isEmpty()) return null;
        return String.join(" / ", gsms);
    }

    private String extractPerson(String line) {
        Matcher m = PERSON_PREFIX.matcher(line);
        if (!m.find()) return null;
        int start = m.end();
        StringBuilder name = new StringBuilder(m.group(1) + " ");
        String[] words = line.substring(start).split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (word.matches("0[5-7]\\d*") || word.contains("@")) break;
            if (word.matches("\\+212.*") || word.matches("^\\d.*")) break;
            if (TITLE_KEYWORDS.contains(word.toUpperCase())) break;
            if (!name.toString().endsWith(" ")) name.append(" ");
            name.append(word);
        }
        String r = name.toString().trim();
        String[] parts = r.split("\\s+");
        if (parts.length >= 2) return r;
        return null;
    }

    private String extractFonction(String line, String personName) {
        if (personName == null) return null;
        int idx = line.indexOf(personName);
        if (idx < 0) return null;
        String after = line.substring(idx + personName.length()).trim();
        after = after.replaceAll("\\s+", " ");
        StringBuilder result = new StringBuilder();
        for (String word : after.split(" ")) {
            if (word.matches("0[5-7]\\d*") || word.contains("@")) break;
            if (word.matches("\\+212.*") || word.matches("^\\d{4,}.*")) continue;
            if (result.length() > 0) result.append(" ");
            result.append(word);
        }
        String r = result.toString().trim();
        return r.length() > 1 && r.length() < 80 ? r : null;
    }

    private String extractActivity(String line, String city, String person) {
        if (city == null || person == null) return null;
        int cityIdx = line.indexOf(city);
        int personIdx = line.indexOf(person);
        if (cityIdx < 0 || personIdx < 0 || personIdx <= cityIdx) return null;
        String raw = line.substring(cityIdx + city.length(), personIdx).trim();
        if (raw.isEmpty() || raw.length() > 120) return null;
        if (raw.matches("^\\d.*") || raw.startsWith("http")) return null;
        return raw;
    }

    private String extractCity(String line) {
        Matcher m = CITY_PAT.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private String extractCompany(String line, String city) {
        if (city == null) return null;
        int idx = line.indexOf(city);
        if (idx > 0) {
            String before = line.substring(0, idx).trim();
            before = before.replaceAll("\\s+:\\s*$", "").trim();
            if (before.length() >= 3) return before;
        }
        return null;
    }
}
