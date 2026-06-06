# Spec fonctionnelle — Refactoring génération PDF relevé bancaire avec i18n

## 1. Contexte

Le module `financial-batch` génère des relevés bancaires PDF via Spring Batch.
Actuellement, le rendu (layout, styles, libellés) est entièrement codé en Java dans
`BankStatementPdfProcessor`. Cette approche rend toute modification de présentation
dépendante d'un cycle compile → redéploiement.

---

## 2. Objectif

Séparer les **données** (entités JPA) du **rendu** (template) afin de :

- Modifier le layout sans toucher au code Java
- Supporter plusieurs langues (fr, en, ar, …) selon la locale du client
- Permettre à terme plusieurs templates (marque blanche, filiale, …)

---

## 3. État actuel (As-Is)

```
BankStatementPdfProcessor
  ├── construit le PDF iText 7 en Java pur
  ├── libellés hardcodés ("RELEVÉ DE COMPTE", "Solde d'ouverture", …)
  ├── styles hardcodés (PdfStatementStyle.java : couleurs, marges, polices)
  └── retourne byte[]
```

**Problèmes :**
- Changer un libellé = recompiler + redéployer
- Pas de support multi-langue
- Couplage fort entre logique métier et présentation

---

## 4. Cible (To-Be)

```
BankStatementPdfProcessor
  └── construit StatementData (DTO pur, sans entité JPA)
        └── PdfTemplateRenderer
              ├── résout la locale (customer.locale → fr/en/ar)
              ├── charge messages_{locale}.properties
              ├── rend le template HTML (Thymeleaf)
              └── convertit HTML → PDF (Flying Saucer / OpenPDF)
  └── retourne StatementPdfResult(statement, byte[])
```

`BankStatementPdfWriter` reste inchangé — il continue d'uploader dans MinIO.

---

## 5. Choix technique

| Composant | Choix | Raison |
|---|---|---|
| Moteur de template | **Thymeleaf** | Natif Spring Boot, syntaxe HTML standard |
| HTML → PDF | **Flying Saucer (XHTML renderer)** | Léger, pas de dépendance externe, supporte CSS |
| i18n | **Spring MessageSource** (`messages_{locale}.properties`) | Standard Spring, facile à étendre |
| DTO de rendu | `StatementData` (record Java) | Découple l'entité JPA du template |

---

## 6. Structure des fichiers

```
financial-batch/src/main/
├── resources/
│   ├── templates/
│   │   └── statement/
│   │       └── bank-statement.html        ← template Thymeleaf unique
│   ├── static/
│   │   └── statement/
│   │       └── statement.css              ← styles externalisés
│   └── i18n/
│       ├── messages_fr.properties         ← français (défaut)
│       ├── messages_en.properties         ← anglais
│       └── messages_ar.properties         ← arabe (optionnel, phase 2)
└── java/.../batch/
    ├── statement/
    │   ├── pdf/
    │   │   ├── StatementData.java         ← DTO de rendu (record)
    │   │   ├── StatementLineData.java     ← DTO ligne (record)
    │   │   ├── PdfTemplateRenderer.java   ← Thymeleaf → HTML → PDF
    │   │   └── StatementPdfResult.java    ← inchangé
    │   └── processor/
    │       └── BankStatementPdfProcessor  ← simplifié : construit StatementData
    └── config/
        └── MessageSourceConfig.java       ← configuration MessageSource
```

---

## 7. Internationalisation

### Résolution de la locale

```
customer.locale  →  "fr" | "en" | "ar"
     ↓
Locale.forLanguageTag(customer.locale)
     ↓
MessageSource.getMessage("statement.title", null, locale)
```

Si `customer.locale` est `null` → fallback sur `fr` (langue par défaut).

### Exemple fichiers properties

**messages_fr.properties**
```properties
statement.title=RELEVÉ DE COMPTE
statement.period=Période
statement.holder=Titulaire
statement.email=Email
statement.rib=RIB
statement.generated.at=Généré le
statement.opening.balance=Solde d''ouverture
statement.closing.balance=Solde de clôture
statement.operations.title=Détail des opérations
statement.col.date=Date
statement.col.number=N° Opération
statement.col.label=Libellé
statement.col.debit=Débit
statement.col.credit=Crédit
statement.col.balance=Solde
statement.legal.footer=Ce relevé est un document officiel. Toute contestation doit être signalée dans un délai de 30 jours.
```

**messages_en.properties**
```properties
statement.title=ACCOUNT STATEMENT
statement.period=Period
statement.holder=Account holder
statement.email=Email
statement.rib=IBAN
statement.generated.at=Generated on
statement.opening.balance=Opening balance
statement.closing.balance=Closing balance
statement.operations.title=Transaction details
statement.col.date=Date
statement.col.number=Transaction No.
statement.col.label=Description
statement.col.debit=Debit
statement.col.credit=Credit
statement.col.balance=Balance
statement.legal.footer=This statement is an official document. Any dispute must be reported within 30 days.
```

---

## 8. DTO de rendu

```java
// StatementData.java
public record StatementData(
    UUID statementId,
    String accountRib,
    String customerFullName,
    String customerEmail,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime generatedAt,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<StatementLineData> lines,
    Locale locale
) {}

// StatementLineData.java
public record StatementLineData(
    LocalDateTime operationDate,
    String operationNumber,
    String label,
    BigDecimal amount,
    OperationType operationType,
    BigDecimal runningBalance
) {}
```

---

## 9. Flux de rendu

```
PdfTemplateRenderer.render(StatementData data)

1. Résoudre la locale → Locale
2. Construire le contexte Thymeleaf
     context.setVariable("data", data)
     context.setVariable("msg", messageSource)  ← ou passer les messages résolus
     context.setLocale(locale)
3. templateEngine.process("statement/bank-statement", context) → String HTML
4. ITextRenderer (Flying Saucer) .setDocumentFromString(html) → byte[]
5. retourner byte[]
```

---

## 10. Template HTML (extrait)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <link rel="stylesheet" th:href="@{/statement/statement.css}"/>
</head>
<body>
  <div class="header">
    <span th:text="#{statement.title}">RELEVÉ DE COMPTE</span>
    <span th:text="${data.periodStart} + ' – ' + ${data.periodEnd}"></span>
  </div>

  <table class="operations">
    <thead>
      <tr>
        <th th:text="#{statement.col.date}">Date</th>
        <th th:text="#{statement.col.number}">N° Opération</th>
        <th th:text="#{statement.col.label}">Libellé</th>
        <th th:text="#{statement.col.debit}">Débit</th>
        <th th:text="#{statement.col.credit}">Crédit</th>
        <th th:text="#{statement.col.balance}">Solde</th>
      </tr>
    </thead>
    <tbody>
      <tr th:each="line : ${data.lines}">
        <td th:text="${line.operationDate}"></td>
        <td th:text="${line.operationNumber}"></td>
        <td th:text="${line.label}"></td>
        <td th:text="${line.operationType == 'DEBIT' ? line.amount : ''}"></td>
        <td th:text="${line.operationType == 'CREDIT' ? line.amount : ''}"></td>
        <td th:text="${line.runningBalance}"></td>
      </tr>
    </tbody>
  </table>
</body>
</html>
```

---

## 11. Phases de livraison

| Phase | Scope | Condition de fin |
|---|---|---|
| **Phase 1** | Introduire `StatementData` + `PdfTemplateRenderer` + template fr uniquement | PDF identique au rendu actuel, tests verts |
| **Phase 2** | Ajouter `messages_en.properties` + résolution `customer.locale` | Job produit PDFs fr/en selon la locale client |
| **Phase 3** | Externaliser `statement.css` + valider rendu Flying Saucer | Mise en page validée sur un vrai PDF |
| **Phase 4** *(optionnel)* | Support arabe (`ar`) + direction RTL dans le CSS | — |

---

## 12. Ce qui ne change pas

- `BankStatementPdfWriter` — upload MinIO inchangé
- `PendingStatementItemReader` — inchangé
- Chunk size, `@StepScope`, `JobParameters` — inchangés
- Clé objet MinIO (`statements/{year}/{month}/...`) — inchangée
  Tout est en place. Voici le récap des changements Phase 1 :

  ┌──────────┬────────────────────────────────────────────────────────────┐                                                                                                                                                                  
  │  Action  │                          Fichier                           │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Supprimé │ iText 7 (kernel, layout, io) dans les deux pom.xml         │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Ajouté   │ spring-boot-starter-thymeleaf + openhtmltopdf-pdfbox/slf4j │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Créé     │ StatementData.java + StatementLineData.java (DTOs records) │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Créé     │ PdfTemplateRenderer.java (Thymeleaf → HTML → PDF)          │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Créé     │ templates/statement/bank-statement.html                    │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Créé     │ i18n/messages_fr.properties                                │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Réécrit  │ BankStatementPdfProcessor.java (30 lignes vs 215)          │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Supprimé │ PdfStatementStyle.java (styles migrés en CSS)              │                                                                                                                                                                  
  ├──────────┼────────────────────────────────────────────────────────────┤                                                                                                                                                                  
  │ Modifié  │ application.yml — ajout spring.messages.basename           │                                                                                                                                                                  
  └──────────┴────────────────────────────────────────────────────────────┘ 
