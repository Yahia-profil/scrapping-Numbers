package com.ceotracker.service.scraper;

import com.ceotracker.entity.CeoContact;
import com.ceotracker.service.PhoneNormalizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PdfScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(PdfScraper.class);
    private static final String PDF_DIR = "C:\\Users\\user\\Desktop\\scrap";

    private static final Pattern PHONE_06_07 = Pattern.compile(
        "(?:0[67]|\\+212\\s*[67]|00212\\s*[67])\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d");

    public PdfScraper() {
        super("PDF Documents", "PDF");
    }

    @Override
    public List<CeoContact> scrape() {
        Set<CeoContact> results = new LinkedHashSet<>();

        try (Stream<Path> paths = Files.list(Paths.get(PDF_DIR))) {
            List<Path> pdfFiles = paths
                .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                .sorted()
                .toList();

            log.info("PDF: {} fichiers trouvés", pdfFiles.size());

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

        List<CeoContact> result = new ArrayList<>(results);
        log.info("PDF total: {} contacts", result.size());
        return result;
    }

    private List<CeoContact> extractFromPdf(File file) {
        List<CeoContact> results = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);

            String[] lines = text.split("[\n\r]+");
            List<String> lineList = new ArrayList<>(Arrays.asList(lines));

            // Find phone numbers with context - scan backwards for company name
            Set<String> seenPhones = new HashSet<>();

            for (int i = 0; i < lineList.size(); i++) {
                String line = lineList.get(i);
                String normalized = line.replaceAll("[\\s\\-–—.()/]+", "");
                Matcher m = PHONE_06_07.matcher(normalized);

                while (m.find()) {
                    String rawDigits = m.group().replaceAll("\\s+", "");
                    String phone = PhoneNormalizer.normalize(rawDigits);
                    if (phone == null || !phone.matches("0[67]\\d{8}")) continue;
                    if (seenPhones.contains(phone)) continue;

                    // Scan backwards up to 3 lines for company name
                    String company = findCompanyNameInContext(lineList, i, phone);
                    if (company == null) continue;

                    seenPhones.add(phone);
                    CeoContact contact = new CeoContact();
                    contact.setCompanyName(company.length() > 100 ? company.substring(0, 100) : company);
                    contact.setPhoneNumber(phone);
                    contact.setSourceType(getSourceType());
                    contact.setLastVerifiedAt(LocalDateTime.now());
                    results.add(contact);
                }
            }
        } catch (Exception e) {
            log.debug("Erreur extraction PDF {}: {}", file.getName(), e.getMessage());
        }
        return results;
    }

    private String findCompanyNameInContext(List<String> lines, int currentLine, String phone) {
        // Scan up to 3 lines before the phone line
        int start = Math.max(0, currentLine - 3);

        // Collect all candidate text from these lines
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = start; i <= currentLine; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            // Remove the phone number itself from the line
            line = line.replaceAll("0[67]\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d", "");
            line = line.replaceAll("\\+212\\s*[67]\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d", "");
            line = line.replaceAll("00212\\s*[67]\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d", "");
            if (line.trim().isEmpty()) continue;
            contextBuilder.append(line).append(" | ");
        }

        String context = contextBuilder.toString();

        // Try splitting by common table separators and find first valid company
        String[] parts = context.split("\\||\\t|\\s{3,}");
        for (String part : parts) {
            String candidate = part.trim();
            if (candidate.isEmpty()) continue;
            // Remove common suffixes that aren't company names
            candidate = candidate.replaceAll("\\s+", " ").trim();

            // Must pass company validation
            if (PageParser.isValidCompany(candidate)) {
                // Additional check: reject if it contains job title words
                if (candidate.toLowerCase().matches(".*(directeur|responsable|manager|g[ée]rant|pr[ée]sident|assistant|chef|superviseur).*"))
                    continue;
                return candidate;
            }
        }

        // Fallback: scan full lines backwards
        for (int i = currentLine - 1; i >= start; i--) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            // Remove phone numbers from line
            line = line.replaceAll("0[567]\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d\\s*\\d", "");
            line = line.replaceAll("\\+212\\s*[567]\\s*\\d{8}", "");
            line = line.replaceAll("00212\\s*[567]\\s*\\d{8}", "");
            line = line.trim();
            if (line.isEmpty()) continue;

            // Try extracting first segment before any long gap or separator
            String[] segments = line.split("\\s{3,}|\\||\\t");
            for (String seg : segments) {
                seg = seg.trim();
                if (seg.isEmpty()) continue;
                if (PageParser.isValidCompany(seg)) {
                    String lower = seg.toLowerCase();
                    if (lower.contains("directeur") || lower.contains("responsable")
                        || lower.contains("manager") || lower.contains("g[ée]rant")
                        || lower.contains("pr[ée]sident"))
                        continue;
                    return seg;
                }
            }
        }

        return null;
    }
}
