package com.barry.bank.api;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configuration commune des tests d'intégration.
 *
 * <p>Toutes les classes d'IT doivent hériter de cette classe : une configuration
 * strictement identique permet à Spring Test de réutiliser un seul contexte
 * (un seul démarrage d'application et un seul cycle Liquibase pour toute la suite).
 *
 * <p>La base PostgreSQL est fournie par Testcontainers (même image que le
 * docker-compose) : conteneur unique démarré une fois pour toute la JVM de test
 * (pattern singleton — pas de {@code @Testcontainers}/{@code @Container}, qui
 * arrêteraient le conteneur entre deux classes alors que le contexte Spring,
 * lui, est mis en cache). {@code @ServiceConnection} injecte url/user/password
 * dans le datasource, plus rien n'est codé en dur dans application-it.yml.
 *
 * <p>Le jeu de données ({@code dataPostgres.sql}) est rechargé avant chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@Sql(scripts = "classpath:scripts/dataPostgres.sql")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:12.8");

    static {
        POSTGRES.start();
    }
}