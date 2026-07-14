# Stub WireMock du service d'archivage — environnement de staging (décision spike #46).
# Simule POST /api/v1/archives/customers → 201 ARCHIVED (voir mappings/).
# L'API admin (/__admin/requests) permet de vérifier les appels reçus en staging.
FROM wiremock/wiremock:3.13.2-alpine

COPY mappings/ /home/wiremock/mappings/

# Render fournit PORT (10000 par défaut) ; en local on retombe sur 8080.
ENTRYPOINT ["/bin/sh", "-c", "exec /docker-entrypoint.sh --port ${PORT:-8080} --verbose"]
