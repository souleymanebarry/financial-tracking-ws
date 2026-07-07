# Seed du Vault dev (docker-compose)

Le Vault du compose tourne en mode dev : **stockage en mémoire, vidé à chaque
redémarrage du conteneur**. Le service `vault-init` re-seed automatiquement le
secret attendu par `financial-api` à chaque `docker compose up`, à partir d'un
fichier local **jamais commité** (`*.pem` est gitignoré dans ce dossier).

## Mise en place (une seule fois par machine)

Dépose ta clé privée RSA (format PKCS8, `-----BEGIN PRIVATE KEY-----`, appariée
avec `financial-api/src/main/resources/certs/public.pem`) dans :

```
docker/vault/rsa-private-key.pem
```

C'est tout — `vault-init` la pousse dans `secret/financial-tracking-ws/rsa`
(clé `rsa-private-key`) à chaque démarrage de la stack.

Si le fichier est absent, `vault-init` échoue avec un message explicite et
`financial-api` ne démarre pas (dépendance `service_completed_successfully`).