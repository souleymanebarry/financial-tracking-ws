# ============================================================
# Stage 1 — Build
# ============================================================
FROM openjdk:17-slim AS builder
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
RUN ./mvnw dependency:go-offline -q

# Copy sources
COPY financial-domain/src         financial-domain/src
COPY financial-persistence/src    financial-persistence/src
COPY financial-application/src    financial-application/src
COPY financial-api/src            financial-api/src
COPY financial-test-support/src   financial-test-support/src
COPY financial-batch/src          financial-batch/src
COPY financial-document/src       financial-document/src

# Build + extract Spring Boot layers (tests run separately in CI)
RUN ./mvnw clean package -pl financial-api -am -DskipTests -q && \
    java -Djarmode=layertools \
         -jar financial-api/target/financial-api-*.jar \
         extract --destination financial-api/target/extracted

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM openjdk:17-slim AS runtime

# Non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app

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

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=Europe/Paris", \
  "-Dsun.net.inetaddr.ttl=60", \
  "org.springframework.boot.loader.launch.JarLauncher"]
