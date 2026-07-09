# ============================================================
# Stage 1 — Build (commun aux deux exécutables)
# ============================================================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Copy Maven wrapper first — cached until wrapper version changes
COPY .mvn/           .mvn/
COPY mvnw            .
RUN chmod +x mvnw

# Copy all POMs before sources to maximise dependency-cache hits
COPY pom.xml                          .
COPY financial-domain/pom.xml         financial-domain/
COPY financial-persistence/pom.xml    financial-persistence/
COPY financial-application/pom.xml    financial-application/
COPY financial-api/pom.xml            financial-api/
COPY financial-test-support/pom.xml   financial-test-support/
COPY financial-batch/pom.xml          financial-batch/
COPY financial-document/pom.xml       financial-document/

# Resolve all dependencies (separate layer — rebuilt only when a POM changes)
RUN ./mvnw -B -ntp dependency:go-offline -q

# Copy sources
COPY financial-domain/src         financial-domain/src
COPY financial-persistence/src    financial-persistence/src
COPY financial-application/src    financial-application/src
COPY financial-api/src            financial-api/src
COPY financial-test-support/src   financial-test-support/src
COPY financial-batch/src          financial-batch/src
COPY financial-document/src       financial-document/src

# Version CI-friendly (-Drevision) : passée par le workflow GHCR depuis le tag vX.Y.Z.
# Déclaré juste avant le RUN qui l'utilise pour ne pas invalider la couche go-offline.
ARG REVISION=0.0.1-SNAPSHOT

# Build + extract Spring Boot layers for both executables (tests run separately in CI)
RUN ./mvnw -B -ntp clean package -pl financial-api,financial-batch -am -DskipTests -Drevision=${REVISION} -q && \
    java -Djarmode=layertools \
         -jar financial-api/target/financial-api-*.jar \
         extract --destination financial-api/target/extracted && \
    java -Djarmode=layertools \
         -jar financial-batch/target/financial-batch-*.jar \
         extract --destination financial-batch/target/extracted

# ============================================================
# Stage 2 — Base runtime commune (user non-root)
# ============================================================
FROM eclipse-temurin:17-jre AS runtime-base

# curl requis par les healthchecks (docker-compose / orchestrateur)
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=Europe/Paris", \
  "-Dsun.net.inetaddr.ttl=60", \
  "org.springframework.boot.loader.launch.JarLauncher"]

# ============================================================
# Stage 3 — Runtime financial-batch  (docker build --target batch)
# ============================================================
FROM runtime-base AS batch

# Spring Boot layered copy: stable layers first → app layer last
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-batch/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-batch/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-batch/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-batch/target/extracted/application/ ./

USER appuser
EXPOSE 8090

# ============================================================
# Stage 4 — Runtime financial-api  (docker build --target api,
# dernier stage = cible par défaut d'un build sans --target)
# ============================================================
FROM runtime-base AS api

# Spring Boot layered copy: stable layers first → app layer last
# → only the last COPY is invalidated on a code-only change
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-api/target/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-api/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-api/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup \
    /workspace/financial-api/target/extracted/application/ ./

USER appuser
EXPOSE 8080