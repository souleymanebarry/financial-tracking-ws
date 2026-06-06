# Spec fonctionnelle — Module `financial-document` (Apache FOP)

## 1. Contexte

La Phase 1 utilisait Thymeleaf + openhtmltopdf (HTML → PDF).
Ce module remplace cette approche par **Apache FOP** (XSL-FO → PDF natif),
dans un module dédié réutilisable par `financial-batch` et `financial-api`.

---

## 2. Pourquoi FOP + module séparé

| Critère | Thymeleaf + openhtmltopdf | Apache FOP |
|---|---|---|
| Rendu | HTML converti en PDF | PDF natif, précis au mm |
| Pagination | Approximative | Contrôle total (en-têtes répétés, sauts de page) |
| Polices | Système / web fonts | TTF embarquées, Unicode garanti |
| Réutilisabilité | Couplé à financial-batch | Module autonome, utilisable partout |
| Licence | Apache 2.0 | Apache 2.0 |

**Module séparé** : la génération PDF n'est pas une responsabilité du batch.
Demain `financial-api` peut exposer `GET /statements/{id}/pdf` sans dupliquer le code.

---

## 3. Place dans l'architecture

```
financial-api    ──┐
                   ├──→  financial-document  →  financial-domain
financial-batch  ──┘
```

`financial-document` ne dépend que de `financial-domain` (entités/enums).
Aucune dépendance Spring Batch, Spring MVC, ou Spring Security.

---

## 4. Structure du module

```
financial-document/
├── pom.xml
└── src/main/
    ├── java/com/barry/bank/document/
    │   ├── FinancialDocumentAutoConfiguration.java  ← @Configuration Spring
    │   ├── statement/
    │   │   ├── StatementData.java                   ← DTO record (données pures)
    │   │   ├── StatementLineData.java               ← DTO record (ligne)
    │   │   └── FopStatementRenderer.java            ← point d'entrée public
    │   └── fop/
    │       ├── FopConfig.java                       ← FopFactory singleton
    │       └── FopRenderer.java                     ← moteur FOP bas niveau
    └── resources/
        ├── fop/
        │   ├── fop.xconf                            ← config FOP (résolution, polices)
        │   └── fonts/
        │       ├── DejaVuSans.ttf
        │       └── DejaVuSans-Bold.ttf
        ├── templates/
        │   └── statement/
        │       └── bank-statement.fo.ftl            ← template Freemarker → XSL-FO
        └── i18n/
            ├── messages_fr.properties
            └── messages_en.properties
```

---

## 5. Pipeline de rendu

```
BankStatement (entité JPA)
  │
  ▼  (financial-batch ou financial-api)
StatementData (DTO record)
  │
  ▼  FopStatementRenderer.render(data, locale)
  │
  ├── 1. Résoudre les messages i18n  →  Map<String, String> labels
  ├── 2. Freemarker : context(data + labels)
  │         + bank-statement.fo.ftl
  │         → String XSL-FO
  │
  └── 3. Apache FOP
            + fop.xconf (polices TTF embarquées)
            → byte[]  (PDF A4)
```

---

## 6. Pourquoi Freemarker pour générer le XSL-FO

XSLT est puissant mais difficile à maintenir. Freemarker produit le même XSL-FO
avec une syntaxe plus lisible :

```xml
<!-- bank-statement.fo.ftl (extrait) -->
<fo:block font-size="16pt" font-weight="bold" color="#ffffff">
    ${labels["statement.title"]}
</fo:block>

<#list data.lines as line>
<fo:table-row background-color="${line?is_odd_item?then('#f9f9f9', '#ffffff')}">
    <fo:table-cell><fo:block>${line.operationDate?string("dd/MM/yyyy")}</fo:block></fo:table-cell>
    ...
</fo:table-row>
</#list>
```

---

## 7. Internationalisation

```java
// FopStatementRenderer
Map<String, String> labels = messageSource
    .getMessages(locale);   // résout toutes les clés statement.*
```

La locale est passée en paramètre de `render(data, locale)`.
Phase 1 : `Locale.FRENCH` uniquement — `messages_fr.properties`.
Phase 2 : `messages_en.properties` + résolution depuis `Customer`.

---

## 8. Polices embarquées (beau design)

FOP embarque les polices TTF dans le PDF — les caractères accentués (é, è, à, ç)
et les symboles (€, N°) sont garantis sur toute plateforme.

**`fop.xconf`** déclare les polices :
```xml
<fonts>
    <font metrics-url="DejaVuSans.xml" kerning="yes" embed-url="fonts/DejaVuSans.ttf">
        <font-triplet name="DejaVu Sans" style="normal" weight="normal"/>
    </font>
    <font metrics-url="DejaVuSans-Bold.xml" kerning="yes" embed-url="fonts/DejaVuSans-Bold.ttf">
        <font-triplet name="DejaVu Sans" style="normal" weight="bold"/>
    </font>
</fonts>
```

**Template** : `font-family="DejaVu Sans"` sur `fo:root`.

---

## 9. API publique du module

```java
// Point d'entrée unique — utilisé par financial-batch et financial-api
@Component
public class FopStatementRenderer {

    // Génère le PDF du relevé pour la locale donnée
    public byte[] render(StatementData data, Locale locale) throws Exception { ... }
}
```

`StatementData` et `StatementLineData` sont les seuls DTOs exposés.
Ils n'ont aucune dépendance vers Spring Batch ou JPA.

---

## 10. Intégration financial-batch

```java
// BankStatementPdfProcessor — après implémentation du module
@Component
@RequiredArgsConstructor
public class BankStatementPdfProcessor implements ItemProcessor<BankStatement, StatementPdfResult> {

    private final FopStatementRenderer renderer;

    @Override
    public StatementPdfResult process(BankStatement statement) throws Exception {
        StatementData data = StatementDataMapper.from(statement);
        byte[] pdf = renderer.render(data, Locale.FRENCH);
        return new StatementPdfResult(statement, pdf);
    }
}
```

---

## 11. Dépendances Maven

```xml
<!-- financial-document/pom.xml -->
<dependencies>
    <!-- Apache FOP -->
    <dependency>
        <groupId>org.apache.xmlgraphics</groupId>
        <artifactId>fop</artifactId>
        <version>2.9</version>
    </dependency>

    <!-- Freemarker — génération XSL-FO -->
    <dependency>
        <groupId>org.freemarker</groupId>
        <artifactId>freemarker</artifactId>
        <version>2.3.33</version>
    </dependency>

    <!-- Domain (StatementLine, OperationType, ...) -->
    <dependency>
        <groupId>com.barry.bank</groupId>
        <artifactId>financial-domain</artifactId>
    </dependency>

    <!-- Spring Context (MessageSource, @Component) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
    </dependency>
</dependencies>
```

---

## 12. Phases de livraison

| Phase | Scope | Condition de fin |
|---|---|---|
| **Phase 1** | Créer le module, FopFactory, template XSL-FO fr, polices DejaVu | PDF généré via Postman, caractères accentués corrects |
| **Phase 2** | i18n `messages_en.properties` + résolution `Customer.locale` | PDF en fr ou en selon le client |
| **Phase 3** | Intégration `financial-api` endpoint `GET /statements/{id}/pdf` | Téléchargement PDF depuis l'API REST |

---

## 13. Ce qui ne change pas

- `BankStatementPdfWriter` — upload MinIO inchangé
- `BankStatementJobConfig` — Step 2 inchangé
- Clé objet MinIO — inchangée
- `StatementPdfResult` — inchangé