# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all modules — rapide (skip compilation + exécution des tests) ~56s
./mvnw clean install -Dmaven.test.skip=true

# Build all modules — avec compilation des tests mais sans exécution ~1:16
./mvnw clean install -DskipTests

# Build un seul module et ses dépendances
./mvnw clean install -pl financial-api -am -Dmaven.test.skip=true
./mvnw clean install -pl financial-batch -am -Dmaven.test.skip=true

# Run the application (requires docker compose up -d for PostgreSQL + Vault)
./mvnw spring-boot:run -pl financial-api -Dspring-boot.run.profiles=local

# Run all tests (unit + integration) — requires Docker for Testcontainers ITs
./mvnw clean verify

# Run unit tests only for a single module
./mvnw test -pl financial-application

# Run integration tests only (failsafe) for a module
./mvnw verify -pl financial-api -Dsurefire.skip=true

# Run a single IT class / method (failsafe)
./mvnw verify -pl financial-api -Dit.test=CustomerControllerIT
./mvnw verify -pl financial-api -Dit.test=CustomerControllerIT#shouldReturnAllCustomers

# Release-style build (CI-friendly version)
./mvnw clean verify -Drevision=1.2.3

# Local full stack (PostgreSQL, Vault seedé, MinIO, api, batch)
docker compose up -d
```

## Module Architecture

7-module Maven project. Dependency flow is strictly one-directional:

```
financial-api    →  financial-application  →  financial-persistence  →  financial-domain
financial-batch  →  financial-application, financial-persistence, financial-document
financial-document  →  financial-domain (enums only)
financial-test-support  (test scope only, used by all modules)
```

| Module | Package root | Role |
|---|---|---|
| `financial-domain` | `com.barry.bank.domain` | JPA entities (`domain.model`), enums (`domain.enumerations`) — no Spring, no repositories |
| `financial-persistence` | `com.barry.bank.persistence` | Spring Data JPA repositories, Liquibase migrations (owner of the whole schema) |
| `financial-application` | `com.barry.bank.application` | Service interfaces + implementations, business logic |
| `financial-api` | `com.barry.bank.api` | `@SpringBootApplication`, REST controllers, DTOs, MapStruct mappers, security, Swagger |
| `financial-batch` | `com.barry.bank.batch` | Spring Batch: monthly bank statements (PDF via financial-document, stored in MinIO) |
| `financial-document` | `com.barry.bank.document` | PDF rendering (Apache FOP + Freemarker), no persistence |
| `financial-test-support` | — | JAR shipping shared test deps (spring-boot-starter-test, WireMock, Testcontainers) + shared IT Liquibase changelog (`db/changelog/db.changelog-it.yaml`) |

**`financial-api` AND `financial-batch` produce executable Spring Boot JARs** (both have spring-boot-maven-plugin repackage + a Docker build target). All other modules produce plain JARs.

## Versioning (CI-friendly)

The reactor version is `${revision}` (default `0.0.1-SNAPSHOT` in the parent POM), resolved
by `flatten-maven-plugin`. A release build passes `-Drevision=X.Y.Z` (derived from a `vX.Y.Z`
git tag in CI). Never hardcode module versions.

## Key Architectural Decisions

**Controllers are split into interface + impl.** `CustomerController.java` declares the API contract (with OpenAPI annotations), `CustomerControllerImpl.java` implements it. New endpoints go in the interface first.

**MapStruct for DTO mapping.** Mappers live in `financial-api/.../mappers/`. `AccountDTO` is a single flat DTO for both account types, discriminated by `accountType`; `AccountMapper` fills the type-specific fields (`overDraft`/`interestRate`) in an `@AfterMapping`. The account type labels live in the `AccountType` enum (`financial-domain`), single source of truth for `@DiscriminatorValue`, API values and the archive payload. All mappers use `unmappedTargetPolicy = ReportingPolicy.ERROR` — unmapped fields must be explicitly `ignore`d. Lombok + MapStruct annotation processors are configured together in the compiler plugin — order matters.

**JWT with RSA keys.** Security uses asymmetric RSA (not HMAC256). The private key comes from Spring Cloud Vault (`secret/financial-tracking-ws/rsa`, key `rsa-private-key`, PKCS8 PEM content) in `local`/`prod`; the public key is a classpath/file resource (`rsa.public-key`). In docker-compose, the `vault-init` service seeds a dev keypair from `docker/dev-certs/` (never use in prod). `KeyUtils.java` parses the PEM; `RsaKeyProperties` binds the public key.

**Database migrations via Liquibase — `financial-persistence` is the single owner.** Master file: `db.changelog-master.yaml`. DDL scripts `DDL/001–007` (007 = Spring Batch metadata tables, guarded by a `MARK_RAN` precondition), DML data scripts `DML/001–186`. Add new changesets by incrementing the sequence. Liquibase runs at startup of `financial-api` in `local` **and** `prod`; `financial-batch` never migrates (`initialize-schema: never` in prod, `always` only in its test profile).

**External archive service** is called via Spring `RestClient` (not `RestTemplate` or Feign). The client is in `financial-api/.../archive/sync/client/ArchiveCustomer.java`. WireMock stubs this in integration tests via `@AutoConfigureWireMock`.

**Actuator** is enabled in `financial-api` and `financial-batch`: only the `health` endpoint is exposed (with liveness/readiness probes) — used by the docker-compose healthchecks.

## Test Strategy

- **Unit tests** (`financial-application`, mappers in `financial-api`): plain JUnit 5, no Spring context, Mockito. Run by surefire (`*Test`).
- **Repository tests** (`financial-persistence`): `@DataJpaTest` with H2 in PostgreSQL mode (profile `h2`).
- **Integration tests** (`*IT`, run by failsafe in `financial-api` and `financial-batch`): `@SpringBootTest` against **Testcontainers PostgreSQL** (`postgres:12.8`, same image as docker-compose). Docker must be running. The container is a JVM-wide singleton started in a static block with `@ServiceConnection` — deliberately **without** `@Testcontainers`/`@Container`, which would stop it between classes while the Spring context is cached. Schema comes from the shared IT Liquibase changelog in `financial-test-support`.
- `financial-api` ITs extend `AbstractIntegrationTest` (single shared Spring context + WireMock + `@Sql` dataset). `financial-batch` IT (`BankStatementJobIT`) declares its own container and uses `initialize-schema: always` for batch metadata.

Test profiles and their datasources:
- `h2` → in-memory H2, `create-drop` schema (persistence module only)
- `it` → `financial-api` ITs, Testcontainers via `@ServiceConnection` (no datasource in yml)
- `test` → `financial-batch` IT, Testcontainers via `@ServiceConnection`
- `local` → PostgreSQL + Vault (not for automated tests)

## Spring Profiles

| Profile | Use case | Vault | DB |
|---|---|---|---|
| `local` | Local dev | enabled | PostgreSQL |
| `prod` | Production | enabled | PostgreSQL (env vars), Liquibase at startup, logs WARN |
| `h2` | Persistence tests | disabled | H2 in-memory |
| `it` | API integration tests | disabled | Testcontainers PostgreSQL |
| `test` | Batch integration test | disabled | Testcontainers PostgreSQL |

Swagger UI is **disabled in `prod`** profile. CSRF is enabled for Swagger in prod.

## Docker

Single multi-stage `Dockerfile`, two runtime targets sharing one Maven builder stage:
`--target api` (port 8080) and `--target batch` (port 8090); the last stage (`api`) is the
default. Base images: `eclipse-temurin:17-jdk` (build) / `17-jre` (runtime, non-root, curl
installed for healthchecks). `docker-compose.yml` runs the full stack: postgres 12.8, Vault
dev (in-memory — secrets are wiped on container restart, re-seeded by `vault-init` on
`docker compose up`), MinIO, api and batch with actuator healthchecks. First boot on a fresh
volume runs the full Liquibase migration (~8-10 min); the api healthcheck `start_period` is 180s.

**CI/CD: GitHub Actions + GHCR.** `.github/workflows/ci-cd.yml` runs `./mvnw clean verify` on
PRs and pushes to `develop`/`master` (JaCoCo reports uploaded as build artifact — nothing is
published), then a **non-blocking SonarCloud analysis** (`sonar:sonar`; quality gate results
arrive via PR decoration + email, never fail the job). Sonar properties (`sonar.organization`,
`sonar.projectKey`, JaCoCo XML report paths) live in the parent POM; JaCoCo reports are
generated in `post-integration-test` so IT coverage is included. Setup and UI config:
`docs/sonarcloud-setup.md`. On `vX.Y.Z` tags only, after verify: jars are deployed to
GitHub Packages and both images are pushed to
`ghcr.io/souleymanebarry/financial-tracking-ws/financial-{api,batch}` tagged `X.Y.Z` +
`latest`, all with `-Drevision=X.Y.Z` (forwarded to the Docker build via the `REVISION`
build-arg). No SNAPSHOT is ever published.

## Migration Context

The multi-module reorganization (from `com.barry.bank.financial.tracking_ws.*` to the
module-specific packages above) is complete on `develop`. The roadmap
(`README-MIGRATION-ROADMAP.md`) documents the phases; a Kafka integration branch
(`feat/cutomer-archive-by-kafka`) exists for future work.
