# Casablanca CEO Tracker

Application Spring Boot de scraping et tracking des CEOs d'entreprises à Casablanca.

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

### 3. Lancer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**.

### 4. Accéder au dashboard

Ouvre ton navigateur sur [http://localhost:8080/dashboard](http://localhost:8080/dashboard).

## Profils Maven

### Debug PDF

Pour exécuter le debugger d'extraction PDF :

```bash
mvn exec:java -P debug
```

## Structure du projet

```
ceo-tracker/
├── src/main/java/com/ceotracker/
│   ├── CeoTrackerApplication.java        # Point d'entrée Spring Boot
│   ├── controller/DashboardController.java
│   ├── entity/CeoContact.java            # Entité JPA
│   ├── repository/CeoContactRepository.java
│   ├── service/
│   │   ├── CeoLookupService.java
│   │   ├── CeoContactService.java
│   │   ├── ScrapingEventService.java
│   │   ├── ScoringService.java
│   │   ├── PhoneNormalizer.java
│   │   └── scraper/                      # Différents scrapers
│   └── scheduler/ScrapingScheduler.java  # Planification automatique
├── src/main/resources/
│   ├── application.properties
│   └── templates/                        # Templates Thymeleaf
└── pom.xml
```

## Technologies

- **Spring Boot 4.1.0** (Web, Data JPA, Thymeleaf)
- **MySQL** avec Hibernate (auto-update)
- **Jsoup** pour le scraping HTML
- **Apache PDFBox** pour l'extraction PDF
