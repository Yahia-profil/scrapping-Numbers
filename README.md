# Casablanca Annuaire Entreprises

Application Spring Boot qui extrait et affiche les contacts d'entreprises (responsables, directeurs, managers) à partir d'un fichier PDF RH.

## Prérequis

- **Java 25** (ou version compatible)
- **Maven 3.9+**
- **MySQL** (via XAMPP, WAMP, ou installé directement)
- Un navigateur web (pour accéder au dashboard)

## Installation

### 1. Cloner le projet

```bash
git clone https://github.com/Yahia-profil/scrapping-Numbers.git
cd scrapping-Numbers/ceo-tracker
```

### 2. Configurer la base de données

Crée une base MySQL nommée `ceo_tracker_db` :

```sql
CREATE DATABASE ceo_tracker_db;
```

Modifie le fichier `src/main/resources/application.properties` si besoin :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ceo_tracker_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

> ⚠️ Par défaut, le user est `root` et le mot de passe est vide (comme pour XAMPP).

### 3. Placer le fichier PDF

Place le fichier PDF contenant les données RH dans le dossier parent :

```
scrap/
├── RH Emails - The bigest data base-1.pdf
└── ceo-tracker/
```

### 4. Lancer l'application

```bash
mvn spring-boot:run
```

Au démarrage, l'application :
1. Crée automatiquement les tables MySQL
2. Extrait les données du PDF (entreprises, contacts, fonctions, GSM, villes)
3. Les insère en base de données (doublons ignorés)

### 5. Accéder au dashboard

Ouvre [http://localhost:8080](http://localhost:8080).

- Par défaut, seuls les contacts de **Casablanca** sont affichés
- Utilise le menu déroulant pour filtrer par ville
- Export CSV filtré disponible

## Données extraites

Le PDF est parsé et les colonnes suivantes sont extraites :

| Colonne | Description |
|---------|-------------|
| **Entreprise** | Nom de la société |
| **Contact** | Nom de la personne (M./Mme/Mlle) |
| **Fonction** | Poste occupé (Directeur, Responsable, etc.) |
| **GSM** | Numéro mobile 06/07 |
| **Ville** | Localisation |

## Structure du projet

```
ceo-tracker/
├── src/main/java/com/ceotracker/
│   ├── CeoTrackerApplication.java        # Point d'entrée Spring Boot
│   ├── controller/DashboardController.java
│   ├── entity/CeoContact.java            # Entité JPA (company, ceoName, jobTitle, city, phone, email)
│   ├── repository/CeoContactRepository.java
│   ├── service/
│   │   ├── CeoContactService.java        # Chargement PDF au démarrage + filtrage ville
│   │   ├── ScrapingEventService.java
│   │   ├── ScoringService.java
│   │   ├── PhoneNormalizer.java
│   │   └── scraper/
│   │       ├── PdfScraper.java           # Extraction du PDF RH Emails
│   │       └── ...
│   └── scheduler/ScrapingScheduler.java
├── src/main/resources/
│   ├── application.properties
│   └── templates/dashboard.html
└── pom.xml
```

## Technologies

- **Spring Boot 4.1.0** (Web, Data JPA, Thymeleaf)
- **MySQL** avec Hibernate (auto-update)
- **Apache PDFBox** pour l'extraction PDF
