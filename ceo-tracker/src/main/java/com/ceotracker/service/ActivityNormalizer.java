package com.ceotracker.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ActivityNormalizer {

    private static final Map<Pattern, String> RULES = new LinkedHashMap<>();

    static {
        RULES.put(pattern(
            "AGROALIMENTAIRE|ALIMENTAIRE|ALIMENTATION|BOISSON|CONFISERIE|TABAC|SUCRE|"
            + "HUILE|FARINE|CONSERVE|BISCUIT|LEVURE|EPICER|VOLAILLE|POISSON|BOULANGER|"
            + "PATISSE|ABATTOIR|VIANDE|FRUITS|LEGUME|LAITIER|MINOTERIE|SEMOULE|PATE|"
            + "BRIOCHE|CHOCOLAT|JUS|SODA|BRASSERIE|ALIMENT.*ANIMALE|BOISSONS GAZEUSES|"
            + "EMBOUTEILLAGE|CONDITIONNEMENT|PRODUCTION TABAC|CARN,?E|AGRICOLE|AGRICULTURE"
        ), "Agroalimentaire");

        RULES.put(pattern(
            "CIMENTERIE|CIMENT|BTP|CONSTRUCTION|CARRIERE|CARRIÈRE|MATERIAU|MATÉRIAU|"
            + "MARBRE|PIERRE|BATIMENT|BÂTIMENT|TRAVAUX|GALVANISATION|ASCENSEUR|"
            + "CHAUDRONNERIE|ARMATURE|FERROVIAIRE|CONDUITE.*EAU|FONTAINE|ABAT.?JOUR|"
            + "MENUISERIE|PLOMBERIE|PEINTURE.*BATIMENT|REVETEMENT|REVÊTEMENT"
        ), "BTP / Construction");

        RULES.put(pattern(
            "ENERGIE|ÉNERGIE|HYDROCARBURE|CARBURANT|RAFFINAGE|PÉTROLIER|PÉTROLE|"
            + "GAZ|CENTRALE THERMIQUE|ELECTRICITÉ|ÉLECTRICITÉ|PRODUCTION.*ÉLECTRICITÉ"
        ), "Énergie / Hydrocarbures");

        RULES.put(pattern(
            "PHARMACEUTIQUE|PHARMA|MÉDICAMENT|MÉDICAMENT|LABORATOIRE|ANALYSE.*MÉDICAL|"
            + "SANTÉ|PARAPHARMACIE|MATÉRIEL.*MÉDICAL|PRODUITS PARAPHARMACEUTIQUES"
        ), "Industrie Pharmaceutique");

        RULES.put(pattern(
            "TRANSPORT|LOGISTIQUE|MESSAGERIE|LIVRAISON|PORT|CABOTAGE|MARITIME|"
            + "NAVALE|FRET|COURSIER|VOYAGE|AÉRIEN|AERIEN"
        ), "Transport / Logistique");

        RULES.put(pattern(
            "MINE|MINIÈRE|MINIERE|MÉTALLURGIE|MÉTALLURGIQUE|MÉTALLURGIQUE|"
            + "ALUMINIUM|ACIER|FONDERIE|MINERAIS|MINÉRAUX|RAFFINAGE|"
            + "ABRASIF|CHAUDRONNERIE|TOLERIE|TÔLERIE|GALVANISATION|ARMATURE"
        ), "Mines / Métallurgie");

        RULES.put(pattern(
            "AUTOMOBILE|AUTO|CÂBLAGE|CABLAGE|PNEUMATIQUE|RACCORD|GARAGE|"
            + "CONCESSION|PIÈCE.*AUTO|PIECE.*AUTO|VEHICULE|VÉHICULE|CARROSSERIE"
        ), "Automobile");

        RULES.put(pattern(
            "ÉLECTRICITÉ|ELECTRICITE|ÉLECTRIQUE|ELECTRIQUE|ÉLECTRONIQUE|ELECTRONIQUE|"
            + "ÉLECTROMÉNAGER|ELECTROMENAGER|RÉFRIGÉRATEUR|RÉFRIGERATEUR|"
            + "MATÉRIEL.?ÉLECTRIQUE|MATERIEL.?ELECTRIQUE|ÉQUIPEMENT.?ÉLECTRIQUE"
        ), "Électricité / Électronique");

        RULES.put(pattern(
            "TÉLÉCOMMUNICATION|TELECOMMUNICATION|TÉLÉCOM|TELECOM|INFORMATIQUE|"
            + "RÉSEAU|RESEAU|INTERNET|DIGITAL|MOBILE|FIBRE|DATA|TÉLÉPHONIE"
        ), "Télécoms / Informatique");

        RULES.put(pattern(
            "FINANCE|BANQUE|ASSURANCE|CRÉDIT|CREDIT|HOLDING FINANCIER|"
            + "BANCAIRE|INVESTISSEMENT|FONDS|TRÉSORERIE|TRESORERIE"
        ), "Finance / Banque / Assurance");

        RULES.put(pattern(
            "GRANDE DISTRIBUTION|DISTRIBUTION|COMMERCE|COMMERCIALISATION|"
            + "IMPORTATION|IMPORT|EXPORT|CENTRE COMMERCIAL|MARCHÉ|MARCHE|"
            + "SUPERMARCHÉ|SUPERMARCHE|SUPERETTE|SUPÉRETTE|DISTRIBUTIONNELLE|"
            + "DOMICILIATION|QUINCAILLERIE"
        ), "Grande Distribution / Commerce");

        RULES.put(pattern(
            "TEXTILE|HABILLEMENT|VÊTEMENT|VETEMENT|CONFECTION|MODE|PRÊT.?À.?PORTER|"
            + "TISSAGE|FILATURE|CUIR|MAROQUINERIE|CHAUSSURE"
        ), "Textile / Habillement");

        RULES.put(pattern(
            "CHIMIE|CHIMIQUE|PEINTURE|COLLE|PRODUIT.?ENTRETIEN|SOLVANT|"
            + "DÉTERGENT|DETERGENT|ENCRE|PLASTIQUE|POLYMÈRE|POLYMERE|"
            + "RÉSINE|RESINE|CAOUTCHOUC|ABRASIF|PARFUM|COSMÉTIQUE|COSMETIQUE"
        ), "Chimie / Peintures / Nettoyage");

        RULES.put(pattern(
            "CARTON|PAPIER|EMBALLAGE|IMPRESSION|IMPRIMERIE|ÉTIQUETTE|ETIQUETTE|"
            + "GRAPHISME|PUBLICITÉ|PUBLICITE|PÂTE.?PAPIER|PATE.?PAPIER"
        ), "Papier / Carton / Emballage");

        RULES.put(pattern(
            "HÔTELLERIE|HOTELLERIE|HÔTEL|HOTEL|TOURISME|RESTAURATION|"
            + "HÉBERGEMENT|HEBERGEMENT|LOISIR|VOYAGE|HÔTELIER"
        ), "Hôtellerie / Tourisme / Restauration");

        RULES.put(pattern(
            "SÉCURITÉ|SECURITE|GARDIENNAGE|SURVEILLANCE|ALARME|INCENDIE|"
            + "PROTECTION|NETTOYAGE|PROPRETÉ|PROPRETE|SALUBRITÉ|SALUBRITE|"
            + "CONSEIL|CONSULTING|ENGINEERING|BUREAU.?ÉTUDE|BUREAU.?ETUDE|"
            + "FORMATION|ENSEIGNEMENT|ÉCOLE|ECOLE|RECRUTEMENT|TRAVAIL TEMPORAIRE"
        ), "Services");

        RULES.put(pattern(
            "PÊCHE|PECHE|AGRICULTURE|AGRICOLE|FORESTIER|ÉLEVAGE|ELEVAGE|"
            + "PISCICULTURE|HALIEUTIQUE|PALMIER|CULTURE|MAROC.*PÊCHE"
        ), "Agriculture / Pêche");
    }

    private static Pattern pattern(String keywords) {
        return Pattern.compile("\\b(" + keywords + ")\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String upper = raw.toUpperCase().replaceAll("[\\s,:;./\\\\()\\[\\]{}]+", " ").trim();
        return RULES.entrySet().stream()
            .filter(e -> e.getKey().matcher(upper).find())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> {
                String fallback = guessFallback(upper);
                if (fallback != null) return fallback;
                return null;
            });
    }

    private static String guessFallback(String upper) {
        // Try splitting on common delimiters and checking first meaningful part
        String[] parts = upper.split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0];
            // If first word is a company name placeholder, try remaining
            if (first.isEmpty() || first.length() <= 2) return null;
        }
        return "Autre";
    }
}
