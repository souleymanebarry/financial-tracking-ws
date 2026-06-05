# 🏦 Financial Tracking WS | API de Service Bancaire

[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)](#)
[![Tests](https://img.shields.io/badge/tests-passed-success?style=flat-square)](#)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3+-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg?style=flat-square)](https://www.oracle.com/java/technologies/javase/)

API RESTful développée avec **Spring Boot** pour la gestion des clients, comptes bancaires et opérations.  
RESTful API built with **Spring Boot** to manage customers, bank accounts, and operations.

---

## 🗂️ Table des matières | Table of Contents
1. [Aperçu / Overview](#-aperçu--overview)
2. [Fonctionnalités / Features](#-fonctionnalités--features)
3. [Règles Métier / Business Rules](#-règles-métier--business-rules)
4. [Scénario de Validation / Validation Scenario](#-scénario-de-validation--validation-scenario)
5. [Prérequis Techniques / Technical Requirements](#-prérequis-techniques--technical-requirements)
6. [Structure du Projet / Project Structure](#-structure-du-projet--project-structure)
7. [Scripts SQL / SQL Scripts](#-scripts-sql--sql-scripts)
8. [Tests d’Intégration / Integration Tests](#-tests-dintégration--integration-tests)
9. [Documentation API / API Documentation](#-documentation-api--api-documentation)
10. [Licence / License](#-licence--license)

---

## 🔍 Aperçu / Overview

**FR :**  
Cette application permet de gérer les clients, comptes bancaires et opérations (crédit/débit).  
Les données sont stockées dans **PostgreSQL** avec **Spring Data JPA** et exposées via **Spring Boot REST API**.

**EN :**  
This application manages customers, bank accounts, and operations (credit/debit).  
Data is stored in **PostgreSQL** using **Spring Data JPA** and exposed via **Spring Boot REST API**.

---

## ✅ Fonctionnalités / Features

- 🧍‍♂️ **FR :** Gestion complète des clients (CRUD)  
  **EN :** Full customer management (CRUD)

- 💳 **FR :** Création et suivi des comptes bancaires  
  **EN :** Creation and tracking of bank accounts

- 💰 **FR :** Gestion des opérations de débit et crédit  
  **EN :** Debit and credit operation handling

- 🧾 **FR :** Validation des emails et identifiants uniques  
  **EN :** Validation of unique IDs and emails

- 🧪 **FR :** Tests d’intégration avec MockMvc et JdbcTemplate  
  **EN :** Integration tests using MockMvc and JdbcTemplate

---

## 📋 Règles Métier / Business Rules

| 🇫🇷 Français | 🇬🇧 English |
|--------------|-------------|
| Un client possède un ou plusieurs comptes bancaires. | A customer can have one or more bank accounts. |
| Chaque compte appartient à un seul client. | Each account belongs to a single customer. |
| Une opération est liée à un compte existant. | An operation is linked to an existing account. |
| Un compte courant peut avoir un découvert autorisé. | A current account may have an authorized overdraft. |
| Un client ne peut pas avoir un email dupliqué. | A customer cannot have a duplicate email. |

---

## 🧪 Scénario de Validation / Validation Scenario

**FR :**
1. Création d’un client `Emily Brown` via `POST /api/v1/customers`
2. Vérification de la création en base PostgreSQL
3. Récupération du client via `GET /api/v1/customers/{id}`
4. Test de mise à jour partielle (`PATCH`)
5. Vérification de la séquence `customer_id_seq` pour éviter les doublons

**EN :**
1. Create a customer `Emily Brown` using `POST /api/v1/customers`
2. Verify persistence in PostgreSQL
3. Retrieve the customer via `GET /api/v1/customers/{id}`
4. Test partial update (`PATCH`)
5. Check `customer_id_seq` to avoid duplicate primary keys

---
OPENSSL CMD
# Keypair
> openssl genrsa -out keypair.pem 2048
# Public Key
> openssl rsa -in keypair.pem -pubout -out public.pem
# Private Key
> openssl pkcs8 -topk8 -inform PEM -nocrypt -in keypair.pem -out private.pem

## 🧱 Prérequis Techniques / Technical Requirements


13. Générer automatiquement clients API

OpenAPI permet ensuite :

génération TypeScript Angular
génération React client
génération Java SDK
Postman collections

Très utilisé en entreprise.

| Composant / Component | Version |
|-----------------------|----------|
| Java | 17+ |
| Spring Boot | 3.3+ |
| PostgreSQL | 15+ |
| Maven | 3.8+ |
| JUnit / AssertJ / MockMvc | latest |
| Lombok | ✅ |
| OpenAPI / Swagger UI | ✅ |

---

## 🚀 Démarrage / Getting Started

### 🇫🇷 Instructions
1. **Cloner le dépôt :**
```bash
git clone https://github.com/souleymanebarry/bank-service-api.git
cd bank-service-api

peut être créer un autre micro service d'archivage lorsqu'on supprimer un customer que
 le nouveau micro service d'archivage stock les archive . 
communication entre les deux micro- service avec Rest Client ou Rest Templete ?

Oui, c’est une approche parfaitement valide ! Voici une analyse détaillée et quelques recommandations :

1️⃣ Contexte

Tu veux supprimer un Customer dans ton microservice principal et archiver ses données dans un microservice dédié à l’archivage.
C’est une bonne pratique pour séparer les responsabilités et pour audit / historique.

2️⃣ Communication entre microservices

Tu as plusieurs options pour que ton microservice principal envoie les données au microservice d’archivage :

a) REST via RestTemplate

RestTemplate est synchrone et bloque le thread jusqu’à la réponse.

Exemple:

@RestController
public class CustomerController {

    private final RestTemplate restTemplate;

    public CustomerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void deleteCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        // 1. envoyer au service d'archivage
        restTemplate.postForObject("http://archive-service/api/archive", customer, Void.class);
        // 2. supprimer le customer
        customerRepository.delete(customer);
    }
}


✅ Simple et direct.

⚠️ Problème: si le service d’archivage est indisponible, la suppression peut échouer.

b) REST via WebClient (Spring 5+)

WebClient est asynchrone et réactif.

Utile si tu veux ne pas bloquer le thread.

WebClient webClient = WebClient.create("http://archive-service");

webClient.post()
         .uri("/api/archive")
         .bodyValue(customer)
         .retrieve()
         .bodyToMono(Void.class)
         .subscribe(); // asynchrone

c) Event-driven / message broker (recommandé en production)

Au lieu de REST direct, le service principal publie un événement CustomerDeleted.

Le service d’archivage écoute cet événement et stocke l’archive.

Avantages:

Découplage fort

Tolérance aux pannes (la suppression continue même si le service d’archivage est indisponible)

Technologies: Kafka, RabbitMQ, ActiveMQ, ou même Spring Cloud Stream.

3️⃣ Recommandation

Pour un prototype ou une intégration simple: RestTemplate ou WebClient suffisent.

Pour un système distribué robuste: privilégier un event-driven approach (pub/sub).

✅ Avantages de cette approche

Découplage complet : le service principal ne dépend pas directement du service d’archivage.

Tolérance aux pannes : si le service d’archivage est down, Kafka conserve l’événement.

Scalabilité : le service d’archivage peut traiter les messages à son rythme.

✅ Bonne pratique:
 laisser le service lancer l’exception et gérer via @ControllerAdvice. Cela centralise le traitement des erreurs.
#tradeoffs

[ financial-tracking-ws ]
     |
     | DELETE /api/v1/customers/{id}
     v
 ┌───────────────────────────────────────────┐
 │ CustomerServiceImpl.deleteCustomer()      │
 │    ├─ Récupère Customer + comptes + ops   │
 │    ├─ Construit CustomerArchiveDTO        │
 │    ├─ Appelle ArchiveClient.archive(...)  │──► [ financial-archive-service ]
 │    └─ Supprime le Customer                │
 └───────────────────────────────────────────┘
| Composant                      | Rôle principal                                                                             |
| ------------------------------ | ------------------------------------------------------------------------------------------ |
| **Controller**                 | Orchestration inter-systèmes : récupère, archive, supprime                                 |
| **CustomerServiceImpl**        | Logique métier interne : lecture, modification, suppression, composition du client complet |
| **CustomerArchiveServiceImpl** | Envoi vers le microservice d’archivage                                                     |
| **ArchiveCustomer**            | Couche technique d’appel HTTP via `RestClient`                                             |
#mind map
#Taille des payload(s) pour kafka limiter à 1Mo
#Temps de traitement cote consumer
#Avoir de Topic par domaine metier
| Étape | Action                                                     | Microservice                 |
| ----- | ---------------------------------------------------------- | ---------------------------- |
| 1     | Récupère `Customer` complet avec ses comptes et opérations | `financial-service`          |
| 2     | Convertit en `CustomerArchiveDTO`                          | `CustomerArchiveServiceImpl` |
| 3     | Envoie via `ArchiveCustomer` (`WebClient`)                 | `financial-service`          |
| 4     | Reçoit la requête REST, mappe en entités et sauvegarde     | `financial-archive-service`  |
| 5     | Supprime ensuite le client principal                       | `financial-service`          |


3️⃣ Bonnes pratiques
Mesurer régulièrement la taille des payloads dans les logs ou via monitoring.
Fixer une taille maximale réaliste en fonction de ton use case
 (souvent quelques Mo max pour un microservice interne).
Combiner limitation côté serveur et côté client.
En cas de dépassement, retourner HTTP 413 (Payload Too Large) pour signaler l’erreur proprement.

4️⃣ Limitation / contrôle des appels
Rate limiting : limiter le nombre de requêtes par seconde pour éviter les surcharges.
Circuit breaker (Resilience4j, Spring Cloud Circuit Breaker) : 
protéger le service d’archivage si le service principal
envoie trop de requêtes ou si un problème réseau survient.

5️⃣ Journalisation sécurisée
Ne pas logguer de données sensibles (emails, tokens, soldes) en clair.
Logguer uniquement les identifiants et métadonnées utiles pour le debug.

6️⃣ Option avancée : service mesh
Avec des infrastructures comme Istio ou Linkerd, tu peux sécuriser tous les échanges internes
 entre services avec mTLS automatique, et appliquer politiques de sécurité centralisées sans toucher le code.

Exemple simple pour ton cas
Le service principal génère un JWT signé avec une clé partagée.
Le service d’archivage vérifie le JWT avant de traiter le CustomerArchiveDTO.
🟡 service authentication (machine-to-machine)
Les appels se font via HTTPS.
Les DTO sont validés avec @Valid.
____________________________________________
🥇 La seule méthode 100 % fiable : envoyer un email de confirmation.

Si tu veux, je peux te fournir :

🚀 fournir une nouvelle feature complet d’implémentation dans (financial-tracking-ws)

📩 un service d’envoi du mail de vérification

🔗 un système de lien / token / expiration

financial-tracking ---> signe avec PRIVATE KEY
financial-archive  ---> vérifie avec PUBLIC KEY

● Ne crée pas le bucket manuellement — le CommandLineRunner dans MinioConfig le crée automatiquement au démarrage de l'application.                                                                                                          
                                                                                                                                                                                                                                             
  La séquence complète :                                                                                                                                                                                                                     
                                                                                                                                                                                                                                             
  1. Démarre le batch                                                                                                                                                                                                                        
  ./mvnw spring-boot:run -pl financial-batch -Dspring-boot.run.profiles=local                                                                                                                                                                
  Au démarrage tu verras dans les logs :                                                                                                                                                                                                     
  Bucket MinIO créé : bank-statements                                                                                                                                                                                                        
                                                                                                                                                                                                                                             
  2. Déclenche le batch via l'endpoint                                                                                                                                                                                                       
  POST http://localhost:8090/batch/statements/trigger?periodStart=2026-05-01&periodEnd=2026-05-31                                                                                                                                            
                                                                                                                                                                                                                                             
  3. Vérifie dans la console MinIO                                                                                                                                                                                                           
                                                                                                                                                                                                                                             
  http://localhost:9001 → Buckets → bank-statements → tu verras les PDFs avec la structure :                                                                                                                                                 
  statements/                                                                                                                                                                                                                                
    2026/                                                                                                                                                                                                                                    
      05/                                                                                                                                                                                                                                    
        {accountId}_2026-05-01_2026-05-31.pdf                                                                                                                                                                                                
                                                                                                                                                                                                                                             
  4. Télécharge un PDF depuis la console pour valider le contenu.  



Oui, très simple — c'est justement l'intérêt de MinIO : il implémente l'API S3 d'AWS à 100%.

Pour migrer, aucune ligne de code à changer dans le writer ou le config. Uniquement de la configuration :

# application-prod.yml
minio:
  endpoint: https://s3.eu-west-3.amazonaws.com  # région AWS
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  bucket: bank-statements-prod

Le MinioClient accepte n'importe quel endpoint S3-compatible — AWS S3, MinIO, OVH Object Storage, Scaleway, etc.

---
Une seule limite : si tu veux exploiter des fonctionnalités AWS natives (IAM roles, S3 Transfer Acceleration, S3 Event Notifications, presigned URLs avec STS...), il faudrait à terme migrer vers le SDK AWS v2 (software.amazon.awssdk:s3). Mais ça représente ~30 lignes de changement dans BankStatementPdfWriter et MinioConfig — rien de structurel.

Pour le moment le SDK MinIO est le bon choix : il couvre 100% du besoin actuel et laisse la porte ouverte.
WARNING! dev mode is enabled! In this mode, Vault runs entirely in-memory
and starts unsealed with a single unseal key. The root token is already
authenticated to the CLI, so you can immediately begin using Vault.

You may need to set the following environment variables:

PowerShell:
    $env:VAULT_ADDR="http://127.0.0.1:8200"
cmd.exe:
    set VAULT_ADDR=http://127.0.0.1:8200

The unseal key and root token are displayed below in case you want to
seal/unseal the Vault or re-authenticate.

Development mode should NOT be used in production installations!


● Oui, exactement. Le flow complet :                                                                                                                                                                                                
                                                                                                                                                                                                                                    
  Step 1 — BankAccountItemReader lit tous les comptes ACTIVATED (sans filtre de compte), BankStatementProcessor génère un relevé par compte pour la période donnée.                                                                 
                                                                                                                                                                                                                                    
  Step 2 — PendingStatementItemReader lit tous les relevés PENDING créés au step 1, génère le PDF et l'upload dans MinIO.                                                                                                           
                                                                                                                                                                                                                                    
  Donc pour une période 2025-05-01 → 2025-05-31, si tu as 5 comptes activés → 5 relevés en DB + 5 PDFs dans MinIO.    
  
--------------------------------------------------
 ├───────────┼────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
 │ Processor │ BankStatementPdfProcessor  │ Génère le PDF via iText 7 en mémoire (ByteArrayOutputStream). Utilise ThreadLocal<NumberFormat> pour la thread-safety. Retourne un StatementPdfResult (record = statement + bytes) │   
 ├───────────┼────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
 │ Writer    │ BankStatementPdfWriter     │ Upload le PDF dans MinIO avec une clé déterministe (statements/{year}/{month}/{accountId}_{start}_{end}.pdf), puis passe le statut PENDING → GENERATED                             │   
 └───────────┴────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘   
                                                                                                                                                                                                                                    
  ---                                                                                                                                                                                                                               
  Déclenchement                                                                                                                                                                                                                     
                                                                                                                                                                                                                                    
  Deux modes :                                                                                                                                                                                                                      
                                                                                                                                                                                                                                    
  1. Scheduler automatique (BankStatementScheduler) — @Scheduled(cron = "${batch.statement.cron}"), calcule automatiquement la période du mois précédent, et vérifie via JobExplorer qu'aucune exécution n'est déjà active avant de 
  lancer.                                                                                                                                                                                                                           
  2. Trigger manuel (BatchTriggerController) — POST /batch/statements/trigger?periodStart=...&periodEnd=..., pour les relances ou tests locaux. Valide que periodStart < periodEnd et que periodEnd n'est pas dans le futur.        
                                                                                                                                                                                                                                    
  ---                                                                                               


● Le mois de mai 2025 est de loin le plus chargé. Utilise cette URL :                                                                                                                                                               
                                                                                                                                                                                                                                    
  POST http://localhost:8090/batch/statements/trigger?periodStart=2025-05-01&periodEnd=2025-05-31                                                                                                                                   
                                                                                                                                                                                                                                    
  Classement des mois les plus denses dans les données de seed :                                                                                                                                                                    
                                                                                                                                                                                                                                    
  ┌─────────┬─────────────────┐                                                                                                                                                                                                     
  │  Mois   │ Nb d'opérations │                                                                                                                                                                                                     
  ├─────────┼─────────────────┤                                                                                                                                                                                                     
  │ 2025-05 │ ~390 000        │                                                                                                                                                                                                     
  ├─────────┼─────────────────┤                                                                                                                                                                                                     
  │ 2025-04 │ ~314 000        │                                                                                                                                                                                                     
  ├─────────┼─────────────────┤                                                                                                                                                                                                     
  │ 2025-03 │ ~285 000        │                                                                                                                                                                                                     
  ├─────────┼─────────────────┤                                                                                                                                                                                                     
  │ 2025-01 │ ~230 000        │                                                                                                                                                                                                     
  └─────────┴─────────────────┘                                                                                                                                                                                                     
                                                                                                                                                                                                                                    
  Si tu veux tester avec moins de volume pour un premier essai, 2025-03 ou 2025-04 sont aussi de bons candidats.      
