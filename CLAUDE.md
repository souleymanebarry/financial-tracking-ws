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

# Run the application (requires Vault + PostgreSQL)
./mvnw spring-boot:run -pl financial-api -Dspring-boot.run.profiles=local

# Run all tests
./mvnw test

# Run tests for a single module
./mvnw test -pl financial-application

# Run a single test class
./mvnw test -pl financial-api -Dtest=CustomerControllerIT

# Run a single test method
./mvnw test -pl financial-api -Dtest=CustomerControllerIT#shouldReturnAllCustomers

# Generate test coverage report (JaCoCo)
./mvnw verify -pl financial-api
```

## Module Architecture

This is a 5-module Maven project migrating from a monolithic to layered architecture. Dependency flow is strictly one-directional:

```
financial-api  →  financial-application  →  financial-persistence  →  financial-domain
                                         ↗
                  financial-application  →  financial-domain
financial-test-support  (test scope only, used by all modules)
```

| Module | Package root | Role |
|---|---|---|
| `financial-domain` | `com.barry.bank.domain` | JPA entities, enums — no Spring, no repositories |
| `financial-persistence` | `com.barry.bank.persistence` | Spring Data JPA repositories, Liquibase migrations |
| `financial-application` | `com.barry.bank.application` | Service interfaces + implementations, business logic |
| `financial-api` | `com.barry.bank.api` | `@SpringBootApplication`, REST controllers, DTOs, MapStruct mappers, security, Swagger |
| `financial-test-support` | — | WireMock helpers, shared test fixtures |

**Only `financial-api` produces an executable Spring Boot JAR.** All other modules produce plain JARs.

## Key Architectural Decisions

**Controllers are split into interface + impl.** `CustomerController.java` declares the API contract (with OpenAPI annotations), `CustomerControllerImpl.java` implements it. New endpoints go in the interface first.

**MapStruct for DTO mapping.** Mappers live in `financial-api/.../mappers/`. The `BankAccountEntityFactory` and `BankAccountDtoFactory` handle polymorphic account type conversion (Current vs Saving). Lombok + MapStruct annotation processors are configured together in the compiler plugin — order matters.

**JWT with RSA keys.** Security uses asymmetric RSA (not HMAC256). Keys are loaded from Spring Cloud Vault in `local`/`prod` profiles, and from classpath in tests. `KeyUtils.java` handles PEM parsing. `RsaKeyProperties` binds the key paths.

**Database migrations via Liquibase.** All schema changes go in `financial-persistence/src/main/resources/db/changelog/`. The master file is `db.changelog-master.yaml`. DDL scripts are numbered `001_*.sql`; DML data scripts run to `184_*`. Add new changesets by incrementing the sequence.

**External archive service** is called via Spring `RestClient` (not `RestTemplate` or Feign). The client is in `financial-api/.../archive/sync/client/ArchiveCustomer.java`. WireMock stubs this in integration tests via `@AutoConfigureWireMock`.

## Test Strategy

- **Unit tests** (`financial-application`): plain JUnit 5, no Spring context, mock repositories with Mockito.
- **Repository tests** (`financial-persistence`): `@DataJpaTest` with H2 in PostgreSQL mode (profile `h2`).
- **Integration tests** (`financial-api`): `@SpringBootTest(webEnvironment = RANDOM_PORT)` with profile `it`, `@Sql` scripts for data setup, WireMock for archive service.

Test profiles and their datasources:
- `h2` → in-memory H2, `create-drop` schema
- `it` → defined in `financial-api/src/test/resources/application-it.yml`
- `local` → PostgreSQL + Vault (not for automated tests)

## Active Migration Context

The current branch `chore/multi-module-migration` is reorganizing source from `com.barry.bank.financial.tracking_ws.*` to the module-specific packages above. When moving files, update the `package` declaration and all `import` statements that reference the old package.

The roadmap (`README-MIGRATION-ROADMAP.md`) defines 7 phases. Planned future modules include `financial-batch` (PDF statements, MinIO) and eventual Kafka integration (`feat/cutomer-archive-by-kafka` branch exists).

## Spring Profiles

| Profile | Use case | Vault | DB |
|---|---|---|---|
| `local` | Local dev | enabled | PostgreSQL |
| `prod` | Production | enabled | PostgreSQL (env vars) |
| `h2` | Persistence tests | disabled | H2 in-memory |
| `it` | API integration tests | disabled | configured in test yml |

Swagger UI is **disabled in `prod`** profile. CSRF is enabled for Swagger in prod.