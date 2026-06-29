package com.ceotracker.service;

import com.ceotracker.entity.CeoContact;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ScoringService {

    public int calculateScore(CeoContact contact) {
        int score = 0;

        // Base: phone is valid personal mobile (06/07)
        if (contact.getPhoneNumber() != null) {
            if (contact.getPhoneNumber().matches("0[67]\\d{8}")) {
                score += 25; // Personal mobile = higher value
            } else if (PhoneNormalizer.isValidFormat(contact.getPhoneNumber())) {
                score += 15;
            }
        }

        // CEO name found = much more valuable
        if (contact.getCeoName() != null && !contact.getCeoName().isBlank()) {
            score += 20;
        }

        // Company name is meaningful (not "Inconnue")
        if (contact.getCompanyName() != null && !contact.getCompanyName().equals("Inconnue")
            && !contact.getCompanyName().isBlank()) {
            score += 10;
        }

        // Source type weight
        String source = contact.getSourceType();
        if (source != null) {
            switch (source) {
                case "SITE_OFFICIEL" -> score += 40; // Company website = gold standard
                case "ANNUAIRE_OFFICIEL" -> score += 25;
                case "GOOGLE_MAPS" -> score += 20;  // Verified on Google Maps
                case "RECHERCHE_WEB" -> score += 15; // Found via web search
                case "RESEAU_SOCIAL" -> score += 15;
                case "PAGES_JAUNES" -> score += 10;
                case "AUTRE" -> score += 5;
            }
        }

        // Verified on company website = big boost
        if (contact.isVerifiedByWebsite()) {
            score += 20;
        }

        // Verified on Google Maps
        if (contact.isVerifiedOnMaps()) {
            score += 15;
        }

        // Cross-referenced on multiple sources
        if (contact.isCrossReferenced()) {
            score += 10;
        }

        // Recent verification
        if (contact.getLastVerifiedAt() != null) {
            long daysSinceVerification =
                java.time.Duration.between(contact.getLastVerifiedAt(), LocalDateTime.now()).toDays();
            if (daysSinceVerification < 7) {
                score += 10;
            } else if (daysSinceVerification < 30) {
                score += 5;
            }
        }

        return Math.min(score, 100);
    }

    public String getLabel(int score) {
        if (score >= 80) return "Haute Confiance";
        if (score >= 50) return "Moyenne";
        if (score >= 20) return "Faible";
        return "Non Validé";
    }
}
