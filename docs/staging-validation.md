# Validation fonctionnelle staging — checklist go/no-go

Cette checklist conditionne la pose d'un tag `vX.Y.Z` : **le tag est un acte de promotion**
([stratégie CI/CD](ci-cd-strategy.md)), il n'est posé qu'une fois toutes les étapes ci-dessous
vertes sur le staging fraîchement déployé. Elle est rédigée pour être **scriptable** : chaque
étape est une commande `curl` avec un résultat attendu vérifiable — elle servira de
spécification aux smoke tests automatisés (#55).

## Pré-requis

- Le merge sur `master` à valider est déployé : job `deploy-staging` vert dans le run CI,
  déploiement terminé côté Render (dashboard → service → *Events*).
- `jq` installé (extraction des champs JSON).

```bash
BASE_URL=https://financial-api-staging.onrender.com
ARCHIVE_STUB_URL=<URL Render du stub WireMock>   # dashboard Render, service wiremock-archive
```

> **Free tier — réveil des services.** L'API et le stub d'archivage tournent en plan Free
> (spin-down après ~15 min d'inactivité). Le premier appel peut prendre jusqu'à ~1 min ;
> l'étape 0 sert aussi de réveil. Réveiller le stub **avant** l'étape 7 : s'il dort au moment
> du DELETE, l'archivage répond 503 et la suppression est refusée (comportement de sécurité
> attendu, mais faux négatif pour la validation).

## Déroulé

Chaque étape indique la commande et le résultat attendu. **Toute divergence = échec de l'étape.**

### 0. Santé des services (et réveil free tier)

```bash
curl -fsS --retry 5 --retry-delay 15 --retry-all-errors "$BASE_URL/actuator/health"
curl -fsS --retry 5 --retry-delay 15 --retry-all-errors -o /dev/null -w "stub: HTTP %{http_code}\n" "$ARCHIVE_STUB_URL/__admin/mappings"
```

**Attendu** : `{"status":"UP",...}` pour l'API ; `stub: HTTP 200` pour le stub WireMock.

### 1. Authentification JWT

```bash
TOKEN=$(curl -fsS -X POST "$BASE_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"customerId": "staging-validation"}' | jq -r .access_token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && echo "token OK"
```

**Attendu** : `token OK` — le token signé RSA est émis (la clé privée `RSA_PRIVATE_KEY` est
correctement configurée côté Render).

### 2. Création d'un client de test

Le parcours crée ses propres données et les supprime à l'étape 7 : il ne dépend pas du seed
et laisse la base propre.

```bash
CID=$(curl -fsS -X POST "$BASE_URL/api/v1/customers" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Staging","lastName":"Check","email":"staging-check@validation.local","gender":"MALE"}' \
  | jq -r .customerId)
echo "customerId=$CID"
```

**Attendu** : HTTP 201, `customerId` non vide (UUID).

### 3. Lecture clients (unitaire + paginée)

```bash
curl -fsS "$BASE_URL/api/v1/customers/$CID" | jq -r .email
curl -fsS "$BASE_URL/api/v1/customers?page=0&size=5" | jq 'length'
```

**Attendu** : `staging-check@validation.local` ; puis `5` (la base seedée contient largement
plus de 5 clients).

### 4. Mise à jour partielle du client

```bash
curl -fsS -X PATCH "$BASE_URL/api/v1/customers/$CID" \
  -H "Content-Type: application/json" \
  -d '{"email":"staging-check-updated@validation.local"}' | jq -r .email
```

**Attendu** : `staging-check-updated@validation.local`.

### 5. Création de comptes (courant + épargne)

```bash
ACC1=$(curl -fsS -X POST "$BASE_URL/api/v1/accounts/$CID/current-account" \
  -H "Content-Type: application/json" \
  -d '{"balance": 1000, "overDraft": 500}' | jq -r .accountId)
ACC2=$(curl -fsS -X POST "$BASE_URL/api/v1/accounts/$CID/saving-account" \
  -H "Content-Type: application/json" \
  -d '{"balance": 1000, "interestRate": 3.5}' | jq -r .accountId)
echo "current=$ACC1 saving=$ACC2"
```

**Attendu** : HTTP 201 sur les deux, deux `accountId` non vides.

### 6. Opérations financières (crédit, débit, virement, historique)

```bash
curl -fsS -X POST "$BASE_URL/api/v1/accounts/$ACC1/credit" \
  -H "Content-Type: application/json" -d '{"amount": 300, "description": "staging validation credit"}'
curl -fsS -X POST "$BASE_URL/api/v1/accounts/$ACC1/debit" \
  -H "Content-Type: application/json" -d '{"amount": 200, "description": "staging validation debit"}'
curl -fsS -X POST "$BASE_URL/api/v1/accounts/transfer" \
  -H "Content-Type: application/json" \
  -d "{\"sourceAccountId\": \"$ACC1\", \"destinationAccountId\": \"$ACC2\", \"amount\": 100}"
curl -fsS "$BASE_URL/api/v1/accounts/$ACC1" | jq -r .balance
curl -fsS "$BASE_URL/api/v1/accounts/$ACC1/operations" | jq 'length'
curl -fsS "$BASE_URL/api/v1/accounts/$ACC1/history?page=0&size=6" | jq -r .accountId
```

**Attendu** : les trois opérations passent (HTTP 200) ; solde final `1000.00` (1000 + 300 −
200 − 100) ; `4` opérations (crédit, débit, les deux jambes du virement étant débit sur ACC1) —
au minimum `3` ; l'historique renvoie `$ACC1`.

### 7. Suppression du client — sécurité JWT + orchestration d'archivage

Seul endpoint protégé (`DELETE /api/v1/customers/**`) : il valide à la fois la chaîne JWT
complète (clé publique RSA) et l'appel au service d'archivage (stub WireMock).

```bash
# Sans token → refusé
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE "$BASE_URL/api/v1/customers/$CID"
# Avec token → archive + suppression
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE "$BASE_URL/api/v1/customers/$CID" \
  -H "Authorization: Bearer $TOKEN"
# Le client n'existe plus
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/api/v1/customers/$CID"
```

**Attendu** : `401`, puis `204`, puis `404`.

### 8. Swagger UI (activé en staging — décision #47)

```bash
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/swagger-ui/index.html"
```

**Attendu** : `200`.

## Critère go/no-go

- **GO** : les 9 étapes (0–8) passent → le tag `vX.Y.Z` peut être posé.
- **NO-GO** : une seule étape en échec → **pas de tag**. Ouvrir une issue avec l'étape,
  la commande et la réponse obtenue, corriger via le flux normal (PR → `develop` → `master`),
  puis rejouer la checklist **entière** sur le nouveau déploiement.

Cas particuliers connus (ne sont pas des no-go applicatifs, mais bloquent la validation
tant qu'ils ne sont pas traités) :

- **Base PostgreSQL Free expirée (30 j)** : re-seed via `pg_dump`/`pg_restore`
  ([procédure](ci-cd-strategy.md#migrations-liquibase--règle-expandcontract)), puis rejouer.
- **Stub d'archivage endormi** à l'étape 7 : le réveiller (étape 0) et rejouer l'étape.
