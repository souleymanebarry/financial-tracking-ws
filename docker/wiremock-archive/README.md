# Stub WireMock — service d'archivage (staging)

Simulation du service d'archivage externe pour l'environnement de **staging**
(décision : spike [#46](https://github.com/souleymanebarry/financial-tracking-ws/issues/46) ;
stratégie : `docs/ci-cd-strategy.md`, matrice réel/simulé).

- **Stub servi** : `POST /api/v1/archives/customers` (avec `Authorization: Bearer …`)
  → `201 {"status":"ARCHIVED"}` — version relâchée du stub des ITs (`CustomerControllerIT`) :
  pas de match sur le body pour ne pas coupler le stub aux données de staging.
- **Réponse non stubbée** (mauvaise URL, header absent) → `404` WireMock → côté API,
  `ArchiveServiceException` et la suppression de client échoue : comportement identique
  à une vraie panne du service d'archivage.
- **Observabilité** : `GET /__admin/requests` liste les appels reçus (vérification de la
  checklist staging #50). Suspendre le service = simuler l'indisponibilité de l'archivage.

## Test local

```bash
docker build -t wiremock-archive docker/wiremock-archive
docker run --rm -p 8085:8080 wiremock-archive

# 201 attendu :
curl -i -X POST http://localhost:8085/api/v1/archives/customers \
  -H "Authorization: Bearer test" -H "Content-Type: application/json" -d '{}'
```

Le port d'écoute suit la variable `PORT` (convention Render), 8080 par défaut.
L'URL du service est injectée dans l'API staging via `ARCHIVE_SERVICE_URL` (profil `staging`, spike #45).

## Test du service déployé (Render)

Le service tourne sur `https://financial-archive-stub.onrender.com` (Root Directory
`docker/wiremock-archive`, branche `master`, auto-deploy off — redéploiement manuel).
Trois vérifications, dans l'ordre :

```bash
BASE=https://financial-archive-stub.onrender.com

# 1) Cas nominal — l'appel que fera ArchiveCustomer : 201 ARCHIVED attendu.
#    "Bearer test" suffit : le stub vérifie la FORME du header (Bearer .*), pas le JWT.
curl -i -X POST "$BASE/api/v1/archives/customers" \
  -H "Authorization: Bearer test" -H "Content-Type: application/json" -d '{}'

# 2) Cas panne — sans Authorization : 404 attendu (aucun stub ne matche).
#    Côté API, un non-2xx devient ArchiveServiceException → la suppression client échoue.
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/api/v1/archives/customers" \
  -H "Content-Type: application/json" -d '{}'

# 3) Observabilité — journal des requêtes reçues par le stub (API admin WireMock).
#    Sert à vérifier, pendant la validation staging, qu'un DELETE client a bien
#    déclenché l'appel d'archivage. Les hits "/" viennent du healthcheck Render.
curl -s "$BASE/__admin/requests"
```

Notes :

- **Cold start** : sur le free tier, le service s'endort après ~15 min d'inactivité ;
  le premier appel peut prendre 30-60 s (le `curl` patiente, prévoir `--max-time 90`).
- **API admin publique** : `/__admin` est accessible à quiconque a l'URL (lecture des
  requêtes, modification des stubs). Acceptable pour un stub de staging aux données
  factices ; durcissement possible via `--admin-api-basic-auth user:pass` dans
  l'entrypoint du `Dockerfile` si besoin.
