# 🏗️ Migration Roadmap — Monolithe vers Architecture Multi-Modules Maven

## 🎯 Objectif

Migrer progressivement l’application `financial-tracking-ws` d’une architecture Maven mono-module vers une architecture multi-modules afin de :

* clarifier l’architecture ;
* isoler le domaine métier ;
* préparer l’application à grossir ;
* faciliter l’ajout des futures features :

    * batch relevé bancaire ;
    * génération PDF ;
    * archivage MinIO ;
    * observabilité ;
    * reporting ;
    * analytics.

---

# 📌 Architecture actuelle

Architecture monolithique Spring Boot classique :

```text
controllers/
services/
repositories/
entities/
dtos/
mappers/
```

Cette architecture fonctionne correctement mais devient difficile à maintenir lorsque le domaine métier et les traitements techniques grossissent.

---

# 🎯 Architecture cible

```text
financial-tracking-parent
│
├── financial-domain
├── financial-persistence
├── financial-application
├── financial-api
└── financial-batch (future)
```

---

# 🧠 Philosophie de migration

La migration sera :

* progressive ;
* sans casse ;
* incrémentale ;
* orientée métier.

Le but est de garder l’application fonctionnelle à chaque étape.

---

# 🟢 PHASE 0 — Stabilisation du monolithe

## 🎯 Objectif

Stabiliser l’application avant la migration.

## ✅ Actions

* vérifier que le projet compile ;
* vérifier que Liquibase fonctionne correctement ;
* vérifier que tous les endpoints REST fonctionnent ;
* nettoyer les packages inutiles ;
* supprimer le code mort ;
* valider la structure actuelle.

## ✅ Résultat attendu

Application stable avant découpage.

---

# 🟡 PHASE 1 — Création du parent Maven

## 🎯 Objectif

Transformer le projet en parent multi-modules Maven.

---

## 📁 Nouvelle structure

```text
financial-tracking-parent/
│
├── pom.xml
│
├── financial-domain/
├── financial-persistence/
├── financial-application/
├── financial-api/
```

---

## ✅ Actions

### 1. Créer le projet parent

Créer un dossier :

```text
financial-tracking-parent
```

---

### 2. Déplacer le projet actuel

Le projet actuel deviendra temporairement :

```text
financial-api
```

---

### 3. Créer le `pom.xml` parent

```xml
<packaging>pom</packaging>
```

---

### 4. Déclarer les modules

```xml
<modules>
    <module>financial-domain</module>
    <module>financial-persistence</module>
    <module>financial-application</module>
    <module>financial-api</module>
</modules>
```

---

## ✅ Résultat attendu

Le projet compile toujours avec une structure multi-modules.

---

# 🟠 PHASE 2 — Création du module DOMAIN

## 🎯 Objectif

Isoler le cœur métier bancaire.

---

## 📦 Module

```text
financial-domain
```

---

## ✅ Contenu du module

### Entities

* BankAccount
* CurrentAccount
* SavingAccount
* Customer
* Operation

### Enums

* AccountStatus
* OperationType
* Gender

### Futures entities

* BankStatement
* StatementLine

---

## ⚠️ Règles importantes

Le module DOMAIN :

❌ ne contient PAS :

* controller ;
* repository ;
* service Spring ;
* configuration Spring.

✔ contient uniquement :

* métier ;
* modèles ;
* enums ;
* value objects.

---

## ✅ Résultat attendu

Le domaine métier devient indépendant.

---

# 🔵 PHASE 3 — Création du module PERSISTENCE

## 🎯 Objectif

Centraliser toute la couche accès données.

---

## 📦 Module

```text
financial-persistence
```

---

## ✅ Contenu du module

### Repositories

* AccountRepository
* CustomerRepository
* OperationRepository

### Configuration

* JPA
* datasource
* transaction
* Liquibase

### Migrations

* changelog Liquibase
* scripts SQL

---

## 🔗 Dépendances

```text
financial-persistence
    ↓
financial-domain
```

---

## ✅ Résultat attendu

La couche DB devient isolée.

---

# 🟣 PHASE 4 — Création du module APPLICATION

## 🎯 Objectif

Centraliser la logique métier.

---

## 📦 Module

```text
financial-application
```

---

## ✅ Contenu du module

### Services métier

* AccountService
* OperationService
* CustomerService

### Futures services

* StatementService
* StatementCalculationService
* PDFGenerationService

### Use cases

* création compte ;
* opérations ;
* calcul soldes ;
* génération relevé.

---

## 🔗 Dépendances

```text
financial-application
    ↓
financial-domain

financial-application
    ↓
financial-persistence
```

---

## ✅ Résultat attendu

Toute la logique métier est isolée.

---

# 🔴 PHASE 5 — Création du module API

## 🎯 Objectif

Isoler la couche REST.

---

## 📦 Module

```text
financial-api
```

---

## ✅ Contenu du module

### REST Controllers

* AccountController
* CustomerController
* OperationController

### DTOs

* requests
* responses

### Mappers

* MapStruct

### REST Exceptions

* handlers
* API errors

---

## 🔗 Dépendances

```text
financial-api
    ↓
financial-application
```

---

## ⚠️ Important

Le module API contiendra :

```java
@SpringBootApplication
```

---

## ✅ Résultat attendu

L’API REST devient une couche légère.

---

# 🟤 PHASE 6 — Introduction du module BATCH (future)

## 🎯 Objectif

Préparer la génération des relevés bancaires.

---

## 📦 Module

```text
financial-batch
```

---

## ✅ Contenu futur

### Scheduling

* @Scheduled
* Quartz Scheduler (optionnel)

### Jobs

* GenerateMonthlyStatementJob

### Batch orchestration

* chargement comptes ;
* calcul soldes ;
* génération PDF ;
* archivage MinIO.

---

## 🔗 Dépendances

```text
financial-batch
    ↓
financial-application
```

---

## ✅ Résultat attendu

Le batch devient indépendant de l’API REST.

---

# ⚙️ PHASE 7 — Nettoyage architecture

## 🎯 Objectif

Finaliser l’architecture.

---

## ✅ Actions

* suppression dépendances inutiles ;
* suppression dépendances circulaires ;
* harmonisation packages ;
* amélioration naming ;
* centralisation versions Maven ;
* optimisation pom parent.

---

# 📊 Architecture finale cible

```text
financial-tracking-parent
│
├── financial-domain
│
├── financial-persistence
│
├── financial-application
│
├── financial-api
│
└── financial-batch
```

---

# 🧠 Règles d’architecture

## DOMAIN

✔ métier uniquement

---

## APPLICATION

✔ logique métier
✔ use cases

---

## PERSISTENCE

✔ DB
✔ JPA
✔ Liquibase

---

## API

✔ REST
✔ DTO
✔ mapping

---

## BATCH

✔ orchestration
✔ scheduling
✔ jobs

---

# ⚠️ Bonnes pratiques importantes

## ✅ Toujours compiler après chaque phase

```bash
mvn clean install
```

---

## ✅ Migration progressive

Ne jamais déplacer tout le code d’un coup.

---

## ✅ Garder application fonctionnelle

Le projet doit rester exécutable après chaque étape.

---

# 🚀 Évolutions futures possibles

Une fois l’architecture stabilisée :

* génération relevés bancaires ;
* génération PDF ;
* stockage MinIO ;
* observabilité Grafana/Prometheus/Loki ;
* notifications ;
* analytics ;
* fraud detection ;
* reporting financier ;
* microservices éventuels.

---

# 🏁 Conclusion

Cette migration permettra :

* une architecture plus propre ;
* une meilleure maintenabilité ;
* une séparation claire des responsabilités ;
* une meilleure évolutivité ;
* une base solide pour les futures fonctionnalités bancaires.
