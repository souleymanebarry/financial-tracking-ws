# Spike #52 — Déployer une image GHCR précise sur Render et rollbacker vers N-1

Résultats du spike : mécanique Render pour la préprod et la prod (déploiement d'images GHCR
versionnées, jamais de build depuis le dépôt — voir [stratégie CI/CD](ci-cd-strategy.md)).
Ce document sert d'input au provisionnement préprod (#53, #54) et au runbook de rollback (#59).

## Conclusions

- **Pas de credentials registry** : les images `ghcr.io/souleymanebarry/financial-tracking-ws/*`
  sont **publiques** (vérifié : pull anonyme du manifest `financial-api:0.1.1` → HTTP 200,
  2026-07-15). Le service Render « Existing Image » se crée sans credential.
- **Mécanisme retenu pour GitHub Actions : API Render** `POST /v1/services/{id}/deploys`
  avec `imageUrl` — elle renvoie un **deploy id** que le job peut suivre jusqu'à `live` avant
  d'enchaîner les smoke tests (#55). Le deploy hook `?imgURL=` reste l'alternative simple
  (conservé pour le staging, où le suivi n'est pas nécessaire).
- **Rollback N-1 = même commande avec le tag précédent.** Nos tags `X.Y.Z` étant immuables
  (publiés une seule fois par la CI), on échappe au piège documenté par Render (« un tag
  redéployé pointe sur le *dernier* push de ce tag »).
- **Secrets à créer** (GitHub Environment `preprod`, puis `production` — #57) :
  `RENDER_API_KEY` + `RENDER_PREPROD_SERVICE_ID` (et plus tard `RENDER_PROD_SERVICE_ID`).

## Déployer un tag précis

Contrainte commune aux deux mécanismes : host, dépôt et nom d'image doivent être **identiques**
à l'image configurée sur le service — seul le tag (ou digest) peut changer.

### API Render (retenu)

```bash
RENDER_API_KEY=<clé API — dashboard Render → Account Settings → API Keys>
SERVICE_ID=srv-...        # visible dans l'URL du service sur le dashboard
IMAGE=ghcr.io/souleymanebarry/financial-tracking-ws/financial-api

# Déclencher le déploiement du tag X.Y.Z → renvoie {"id": "dep-...", ...}
DEPLOY_ID=$(curl -fsS -X POST "https://api.render.com/v1/services/$SERVICE_ID/deploys" \
  -H "Authorization: Bearer $RENDER_API_KEY" -H "Content-Type: application/json" \
  -d "{\"imageUrl\": \"$IMAGE:X.Y.Z\"}" | jq -r .id)

# Suivre jusqu'au statut final (live | update_failed | build_failed | canceled)
while :; do
  STATUS=$(curl -fsS "https://api.render.com/v1/services/$SERVICE_ID/deploys/$DEPLOY_ID" \
    -H "Authorization: Bearer $RENDER_API_KEY" | jq -r .status)
  echo "deploy $DEPLOY_ID: $STATUS"
  case "$STATUS" in live) exit 0;; *failed|canceled|deactivated) exit 1;; esac
  sleep 15
done
```

### Deploy hook avec `imgURL` (alternative — utilisé en staging sans le paramètre)

```bash
curl -fsS "$DEPLOY_HOOK&imgURL=ghcr.io%2Fsouleymanebarry%2Ffinancial-tracking-ws%2Ffinancial-api%3AX.Y.Z"
```

Valeur d'`imgURL` **URL-encodée** (`/`→`%2F`, `:`→`%3A`). Réponses : `200` (deploy id dans le
JSON), `202` (un deploy tourne déjà, mise en file), `400` (`imgURL` invalide), `409` (service
suspendu). Limites face à l'API : le hook est un secret par service (à régénérer s'il fuit),
et le suivi du deploy exige de toute façon une clé API — autant utiliser l'API partout où le
résultat conditionne la suite.

## Rollback vers N-1

1. **Procédure nominale** : relancer le déploiement avec le tag précédent (commande API
   ci-dessus avec `X.Y.(Z-1)` — le « rollback » n'est qu'un deploy vers l'arrière).
   Render ne met en service la nouvelle instance qu'après **healthcheck vert**
   (`/actuator/health`) : l'ancienne version continue de servir pendant la bascule, et
   **reste en place si le rollback échoue** (statut `update_failed`).
2. **Bouton « Rollback » du dashboard** : réutilise les artefacts des deploys récents (plus
   rapide), et désactive l'auto-deploy par sécurité — sans effet ici, notre auto-deploy est
   déjà off. Utilisable en dépannage manuel ; la procédure scriptée reste la référence.
3. **Préconditions** : l'image N-1 doit toujours exister dans GHCR (Render ne met pas en
   cache les images pullées), et les migrations doivent respecter
   [expand/contract](ci-cd-strategy.md#migrations-liquibase--règle-expandcontract) — sinon
   **ne pas rollbacker**, escalader (runbook #59).

> Point à confirmer lors du test réel : le comportement zéro-downtime documenté par Render
> sur le plan **Free** (spin-down, une seule instance) — chronométrer la bascule et noter
> toute interruption.

## Secrets GitHub Actions

| Secret | Portée | Contenu |
|---|---|---|
| `RENDER_API_KEY` | Environment `preprod` (puis `production`) | Clé API Render (Account Settings → API Keys) |
| `RENDER_PREPROD_SERVICE_ID` | Environment `preprod` | `srv-...` du service préprod |
| `RENDER_STAGING_DEPLOY_HOOK` | déjà en place (staging) | inchangé — le hook suffit quand on ne suit pas le résultat |

## Test sur service jetable (à dérouler avant #53/#54)

Timebox ~1 h, tout se supprime à la fin :

1. Dashboard Render → **New Web Service → Existing Image** →
   `ghcr.io/souleymanebarry/financial-tracking-ws/financial-api:0.1.0`, plan Free, région EU,
   healthcheck `/actuator/health`, auto-deploy off. Env vars : copier celles du service
   staging (profil `staging`, datasource de la base staging — sans risque, Liquibase y est
   désactivé).
2. Créer la clé API, noter le `srv-...` du service jetable.
3. Dérouler la commande API avec le tag `0.1.1` → statut `live`, vérifier
   `/actuator/health` et le tag affiché dans le dashboard (*Events*).
4. **Rollback** : même commande avec `0.1.0` → `live` ; chronométrer, noter le comportement
   pendant la bascule (santé en continu : `while :; do curl -s .../actuator/health; sleep 5; done`).
5. **Cas d'échec** : déployer un tag inexistant (`9.9.9`) → constater `update_failed` et que
   la version en place continue de servir.
6. Supprimer le service jetable ; reporter les mesures (durées, interruption éventuelle)
   dans ce document et cocher les critères de #52.
