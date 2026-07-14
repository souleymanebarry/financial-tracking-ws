# Stratégie CI/CD

Évolution progressive du pipeline : trois environnements sur Render (staging, pré-production,
production), promotion d'un artefact unique et rollback simple. Ce document est la référence ;
le backlog d'implémentation vit dans le GitHub Project
[Financial Tracker](https://github.com/users/souleymanebarry/projects/2) (épics #40, #44, #51, #56).

## Principes

- **Build once, promote** : l'image validée en pré-production est déployée telle quelle en
  production (même tag GHCR). La production n'exécute jamais un build fait depuis le dépôt.
- **Validation progressive** : staging valide vite (à chaque merge), la préprod valide juste
  (image versionnée + smoke tests), la prod ne reçoit que du validé (approbation manuelle).
- **Le tag est un acte de promotion** : il n'est posé qu'après validation fonctionnelle en
  staging, jamais avant.
- **Simplicité d'abord** : rollback manuel, préprod volontairement simplifiée. On automatise
  après stabilisation (voir [Évolutions prévues](#évolutions-prévues)).

## Environnements

| | Staging | Pré-production | Production |
|---|---|---|---|
| Rôle | Validation fonctionnelle rapide | Validation de l'image avant prod | Exécution |
| Déclencheur | Merge sur `master`, **après CI verte** (#49) | Tag `vX.Y.Z`, après `publish` (#54) | Approbation manuelle après smoke tests verts (#58) |
| Artefact | Build Render depuis le dépôt (`Dockerfile --target api`) | Image GHCR `financial-api:X.Y.Z` | **La même** image GHCR `X.Y.Z` |
| Base de données | PostgreSQL Render (dédiée) | PostgreSQL Render (dédiée) | PostgreSQL Render (dédiée) |
| Profil Spring | `staging` (#47) | à trancher : `staging` durcie ou `preprod` dédié (#53) | `prod` |
| Validation | Checklist manuelle go/no-go (#50) | Smoke tests automatisés (#55) | Vérification post-déploiement (health + version) |

### Matrice réel / simulé

| Composant | Staging | Pré-production | Production |
|---|---|---|---|
| Service d'archivage | **Simulé** — WireMock standalone en service Render dédié, image `docker/wiremock-archive/` (décision #46) | Simulé en v1 (même stub WireMock) — écart avec la prod assumé | **Réel** |
| Batch (relevés bancaires) | **Désactivé** | **Désactivé** (v1) | Hors périmètre v1 (voir #60) |
| Stockage objet (MinIO/S3) | Absent (inutile sans batch) | Absent (v1) | Hors périmètre v1 — décision R2/S3/B2 à prendre avec le batch (#60) |
| Vault | **Absent** — secrets via env vars / secret files Render (spike #45) | Absent — même mécanique que staging | À trancher au provisionnement (#58) : même mécanique Render, ou Vault managé |
| Swagger UI | Activé (a priori, à confirmer en #47) | À trancher | **Désactivé** (profil `prod`) |

> Règle : tout écart entre pré-production et production doit être **écrit dans ce tableau**,
> jamais implicite. Les conclusions des spikes #45, #46 et #52 mettent ce document à jour.

## Flux du pipeline

```text
PR / develop
      │
      ▼
   CI (verify : build + tests unit/IT + Sonar, dependency-review, docker-build)
      │
Merge → master
      │  CI verte requise
      ▼
   STAGING          Build Render depuis le dépôt
      │
Validation fonctionnelle (checklist go/no-go)
      │
Tag vX.Y.Z
      │
      ▼
   PUBLISH          jars → GitHub Packages, images api/batch → GHCR (X.Y.Z)
      │
      ▼
   PRÉ-PRODUCTION   Image GHCR X.Y.Z
      │
Smoke tests automatisés
      │
Approbation manuelle (GitHub Environment `production`, required reviewers)
      │
      ▼
   PRODUCTION       La même image GHCR X.Y.Z
```

Mise en œuvre dans `.github/workflows/ci-cd.yml` : `verify` et `publish` existent ;
`deploy-staging` (#49), `deploy-preprod` (#54), `smoke-preprod` (#55) et `deploy-prod` (#58)
arrivent avec les épics correspondantes. Les secrets de déploiement sont rangés par
GitHub Environment (`staging`, `preprod`, `production`) — #57.

## Migrations Liquibase — règle expand/contract

Liquibase s'exécute au démarrage de `financial-api` dans les environnements déployés,
**à l'exception du staging** (voir encadré ci-dessous).
Le rollback se faisant par **redéploiement de l'image N-1 sur une base déjà migrée en N**,
toute migration doit rester compatible avec la version applicative précédente :

- **Expand d'abord** : ajouter (table, colonne nullable, index) sans casser l'existant.
  La version N-1 doit pouvoir tourner sur le schéma N.
- **Contract plus tard** : supprimer ou renommer (colonne, table, contrainte NOT NULL sur
  colonne existante) uniquement dans une version **ultérieure**, une fois qu'aucune version
  déployable n'utilise plus l'ancien schéma.
- Un renommage se fait en deux temps : nouvelle colonne + double écriture, puis suppression
  de l'ancienne dans une version suivante.

**Cette règle est une exigence de revue de code** : sans elle, la stratégie de rollback
ci-dessous est illusoire. Cas limites et procédure détaillée : runbook de rollback (#59).

> **Exception staging (contrainte free tier — #48).** Le parsing du changelog (~646 Mo
> de scripts DML) exige plus de 1 Go de heap : impossible dans l'instance Render 512 Mo
> (OOM au boot, même sans changeset à appliquer). En staging, `spring.liquibase.enabled: false` ;
> la base est gérée par **re-seed** depuis la base locale migrée (docker compose) :
>
> ```bash
> docker exec financial-postgres pg_dump -U financial_user -Fc --no-owner --no-privileges financial_db > staging-seed.dump
> docker exec -i financial-postgres pg_restore -d "$RENDER_EXTERNAL_URL" \
>   --no-owner --no-privileges --clean --if-exists --single-transaction < staging-seed.dump
> ```
>
> Une seule procédure couvre trois cas : le seed initial, l'arrivée de nouveaux changesets
> (appliqués localement par le compose, puis re-seed) et l'expiration de la base Free (30 j).
> **Correctif de fond au backlog** : squasher les DML en baseline (le changelog ne garde que
> le schéma, les données deviennent un seed pg_dump officiel) — il conditionne aussi le
> dimensionnement mémoire de la préprod et de la prod, et la taille de l'image (~244 Mo de
> jar dont l'essentiel est du SQL embarqué). Le volume de données en base est inchangé.

## Stratégie de rollback (v1 : manuelle)

1. Déploiement de la nouvelle image (préprod puis prod).
2. Smoke tests (automatisés en préprod, vérification post-déploiement en prod).
3. En cas d'anomalie : **redéploiement de la dernière image stable** (tag N-1 dans GHCR)
   depuis l'interface Render — procédure pas-à-pas dans le runbook (#59, alimenté par le
   spike #52).

Limite connue : le rollback est sûr tant que les migrations respectent expand/contract.
Si une migration destructive a été appliquée, **ne pas rollbacker** — escalader (cf. runbook).

L'automatisation (smoke tests en échec ⇒ redéploiement N-1 + notification) est volontairement
reportée après stabilisation (#60) : on n'automatise qu'après avoir éprouvé la procédure
manuelle sur de vrais incidents.

## Gestion des releases

- **Tags `vX.Y.Z`** : déclenchent `publish` (jars → GitHub Packages, images → GHCR).
  Ils peuvent marquer des versions intermédiaires ou techniques — toute image destinée à
  être déployée passe par un tag.
- **GitHub Releases** : jamais créées automatiquement. Réservées aux versions majeures et
  aux jalons, créées manuellement une fois la version en production :

  ```bash
  gh release create vX.Y.Z --verify-tag --generate-notes
  ```

L'historique des releases reste ainsi lisible : peu d'entrées, uniquement les versions
importantes, notes générées depuis les commits/PRs depuis le tag précédent.

## Évolutions prévues

Regroupées dans le ticket icebox [#60](https://github.com/souleymanebarry/financial-tracking-ws/issues/60) —
rollback automatisé, tests E2E, tests de charge et de performance, préprod enrichie
(batch réel + stockage objet, archivage réel), notifications d'équipe. Règle de sortie :
un item n'en sort que lorsque la chaîne v1 a tourné sans friction sur plusieurs releases
consécutives.
