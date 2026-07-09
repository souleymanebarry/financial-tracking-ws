# SonarCloud — intégration et configuration

Analyse de qualité du code sur chaque PR et chaque push `develop`/`master`, pilotée par la CI
(`sonar:sonar` dans le job `verify` de `.github/workflows/ci.yml`). L'analyse est
**non bloquante** : le job ne dépend pas du quality gate ; le résultat arrive en décoration
de PR et par notification email.

- Projet : <https://sonarcloud.io/project/overview?id=souleymanebarry_financial-tracking-ws>
- Clés (déclarées dans le `pom.xml` parent, propriétés `sonar.*`) :

| Propriété | Valeur |
|---|---|
| `sonar.organization` | `souleymanebarry` |
| `sonar.projectKey` | `souleymanebarry_financial-tracking-ws` |
| `sonar.host.url` | `https://sonarcloud.io` |

## Mise en place initiale (une seule fois, dans l'interface)

1. **Compte** : [sonarcloud.io](https://sonarcloud.io) → *Log in with GitHub*.
2. **Organisation** : *Import an organization from GitHub* → installer l'app GitHub
   SonarCloud sur le compte `souleymanebarry` avec accès au repo
   `financial-tracking-ws`. Plan **Free** (repo public).
3. **Projet** : *Analyze new project* → cocher `financial-tracking-ws`.
4. **Désactiver l'analyse automatique** — indispensable : projet →
   **Administration → Analysis Method** → décocher **Automatic Analysis**.
   L'analyse automatique (côté serveur SonarCloud) est incompatible avec l'analyse
   CI (scanner Maven) : si les deux sont actives, le `sonar:sonar` de la CI échoue avec
   `You are running CI analysis while Automatic Analysis is enabled`.
5. **Vérifier les clés** : page du projet → **Administration → Update Key**. Si la
   `projectKey` ou l'organisation diffèrent des valeurs du tableau ci-dessus, corriger
   les propriétés `sonar.organization` / `sonar.projectKey` dans le `pom.xml` parent.
6. **Token** : My Account → **Security** → *Generate token* (user token).
7. **Secret GitHub** : repo GitHub → Settings → Secrets and variables → Actions →
   *New repository secret* : nom `BANK_WS_SONAR_TOKEN`, valeur = le token généré.
   La CI l'expose au scanner via la variable d'environnement `SONAR_TOKEN`.
   Sans ce secret, l'étape *SonarCloud analysis* de la CI échoue (`Not authorized`).

## Notifications email

My Account → **Notifications** → section *Projects* → ajouter `financial-tracking-ws`,
puis cocher :

- **New quality gate status** — email à chaque **changement** de statut du quality gate,
  dans les deux sens : passage au rouge *et* retour au vert. SonarCloud ne notifie que
  les changements : un projet qui reste vert n'envoie aucun email (silence = vert).
- Optionnel : *New issues assigned to me*, *My new issues*.

L'email part à l'adresse du compte GitHub associé.

## Ce que fait la CI

- Le job `verify` fait un checkout `fetch-depth: 0` (historique complet requis pour le
  blame et la détection du *new code*), build `clean verify`, puis `sonar:sonar`.
- La couverture vient des rapports XML JaCoCo de tous les modules
  (`sonar.coverage.jacoco.xmlReportPaths`), générés en phase `post-integration-test`
  pour inclure les tests d'intégration failsafe en plus des tests unitaires.
- L'étape ne tourne **ni sur les tags** (le code a déjà été analysé sur la branche),
  **ni pour dependabot** (ses PRs n'ont pas accès au secret `BANK_WS_SONAR_TOKEN`).
- Le plugin `sonar-maven-plugin` est pinné dans le `pom.xml` parent
  (`sonar-maven-plugin.version`).

## Quality gate

Le gate par défaut *Sonar way* s'applique au **nouveau code** (conditions : couverture,
duplication, notes fiabilité/sécurité/maintenabilité). Il est **informatif** : un gate
rouge n'échoue pas le job CI. Pour le rendre bloquant plus tard, deux options :

- ajouter `-Dsonar.qualitygate.wait=true` à l'étape *SonarCloud analysis* (le job échoue
  si le gate est rouge), et/ou
- exiger le check *SonarCloud Code Analysis* dans la branch protection GitHub.

## IntelliJ — Connected Mode (SonarQube for IDE, ex-SonarLint)

Le plugin analyse à la volée sans configuration, mais avec ses règles par défaut. Pour
utiliser **les mêmes règles que la CI** (quality profile du projet, faux positifs marqués
dans SonarCloud masqués localement, définition du *new code* alignée) :

1. Settings → Tools → **SonarQube for IDE** → *Connections* → ajouter une connexion
   **SonarCloud** (token user, le même type qu'en CI).
2. Lier le projet à `souleymanebarry_financial-tracking-ws` — le plugin détecte
   `sonar.projectKey` dans le `pom.xml` parent et **propose le binding automatiquement**
   (popup *Connect to SonarCloud?*) ; il suffit d'accepter.

La couverture n'apparaît pas dans le plugin — elle reste côté CI (JaCoCo → SonarCloud).

## Analyse locale (optionnel)

```bash
SONAR_TOKEN=<token> ./mvnw clean verify sonar:sonar
```

Analyse la copie de travail locale ; ne pas en faire une habitude sur `master`
(l'historique des analyses doit refléter la CI).
