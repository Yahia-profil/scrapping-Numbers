package com.ceotracker.service;

import java.text.Normalizer;
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
            + "EMBOUTEILLAGE|CONDITIONNEMENT|PRODUCTION TABAC|CARN.?E|AGRICOLE|AGRICULTURE|"
            + "PECHE|FORESTIER|ELEVAGE|PISCICULTURE|HALIEUTIQUE|PALMIER|CULTURE|"
            + "CONFISERIE|CONFISERIES|"
            + "AGROALIMENTAIRE.*AGRICULTURE"
        ), "Agroalimentaire & Agriculture");

        RULES.put(pattern(
            "CIMENTERIE|CIMENT|BTP|CONSTRUCTION|CARRIERE|MATERIAU|"
            + "MARBRE|PIERRE|BATIMENT|TRAVAUX|GALVANISATION|ASCENSEUR|"
            + "CHAUDRONNERIE|ARMATURE|FERROVIAIRE|CONDUITE.*EAU|FONTAINE|ABAT.?JOUR|"
            + "MENUISERIE|PLOMBERIE|REVETEMENT|"
            + "MINE|MINIERE|METALLURGIE|METALLURGIQUE|ALUMINIUM|ACIER|FONDERIE|"
            + "MINERAIS|MINERAUX|ABRASIF|TOLERIE|"
            + "ENERGIE|HYDROCARBURE|CARBURANT|RAFFINAGE|PETROLIER|PETROLE|"
            + "GAZ|CENTRALE THERMIQUE|"
            + "AUTOMOBILE|AUTO|CABLAGE|PNEUMATIQUE|RACCORD|GARAGE|"
            + "CONCESSION|PIECE.*AUTO|VEHICULE|CARROSSERIE|"
            + "FOURNITURE|MATERIEL|EQUIPEMENT|MAINTENANCE|POMPE|"
            + "ENGIN|SIGNALISATION|TEMPORISATEUR|GRAVURE"
        ), "Industrie Lourde");

        RULES.put(pattern(
            "PHARMACEUTIQUE|PHARMA|MEDICAMENT|LABORATOIRE|ANALYSE.*MEDICAL|"
            + "SANTE|PARAPHARMACIE|MATERIEL.*MEDICAL|"
            + "CHIMIE|CHIMIQUE|PEINTURE|COLLE|PRODUIT.?ENTRETIEN|SOLVANT|"
            + "DETERGENT|ENCRE|PLASTIQUE|POLYMERE|"
            + "RESINE|CAOUTCHOUC|PARFUM|COSMETIQUE|PRODUIT.?ENTRETIEN|"
            + "CARTON|PAPIER|EMBALLAGE|IMPRESSION|IMPRIMERIE|ETIQUETTE|"
            + "GRAPHISME|PUBLICITE|PATE.?PAPIER|"
            + "TEXTILE|HABILLEMENT|VETEMENT|CONFECTION|MODE|PRET.?A.?PORTER|"
            + "TISSAGE|FILATURE|CUIR|MAROQUINERIE|CHAUSSURE"
        ), "Industrie Légère");

        RULES.put(pattern(
            "ELECTRICITE|ELECTRIQUE|ELECTRONIQUE|"
            + "ELECTROMENAGER|REFRIGERATEUR|MATERIEL.?ELECTRIQUE|"
            + "TELECOMMUNICATION|TELECOM|INFORMATIQUE|"
            + "RESEAU|INTERNET|DIGITAL|MOBILE|FIBRE|DATA|TELEPHONIE|NUMERIQUE|"
            + "MATERIEL.*TELECOMMUNICATION|ULTRANET|TYPSA|TECHNITEST"
        ), "Numérique & Électricité");

        RULES.put(pattern(
            "TRANSPORT|LOGISTIQUE|MESSAGERIE|LIVRAISON|PORT|CABOTAGE|MARITIME|"
            + "NAVALE|FRET|COURSIER|VOYAGE|AERIEN"
        ), "Transport & Logistique");

        RULES.put(pattern(
            "FINANCE|BANQUE|ASSURANCE|CREDIT|HOLDING FINANCIER|"
            + "BANCAIRE|INVESTISSEMENT|FONDS|TRESORERIE|"
            + "GRANDE DISTRIBUTION|DISTRIBUTION|COMMERCE|COMMERCIALISATION|"
            + "IMPORTATION|IMPORT|EXPORT|CENTRE COMMERCIAL|MARCHE|"
            + "SUPERMARCHE|SUPERETTE|DISTRIBUTIONNELLE|"
            + "DOMICILIATION|QUINCAILLERIE|"
            + "SECURITE|GARDIENNAGE|SURVEILLANCE|ALARME|INCENDIE|"
            + "PROTECTION|NETTOYAGE|PROPRETE|SALUBRITE|"
            + "CONSEIL|CONSULTING|ENGINEERING|BUREAU.?ETUDE|"
            + "FORMATION|ENSEIGNEMENT|ECOLE|RECRUTEMENT|TRAVAIL TEMPORAIRE|BUREAU.?ETUDE|"
            + "HOTELLERIE|HOTEL|TOURISME|RESTAURATION|"
            + "HEBERGEMENT|LOISIR"
        ), "Commerce, Finance & Services");
    }

    private static Pattern pattern(String keywords) {
        return Pattern.compile("\\b(?:" + keywords + ")", Pattern.CASE_INSENSITIVE);
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase()
            .replaceAll("[\\s,:;./\\\\()\\[\\]{}]+", " ")
            .trim();
        return RULES.entrySet().stream()
            .filter(e -> e.getKey().matcher(cleaned).find())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("Autre");
    }
}
