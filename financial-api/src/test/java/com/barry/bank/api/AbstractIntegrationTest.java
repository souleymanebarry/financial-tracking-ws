package com.barry.bank.api;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * Configuration commune des tests d'intégration.
 *
 * <p>Toutes les classes d'IT doivent hériter de cette classe : une configuration
 * strictement identique permet à Spring Test de réutiliser un seul contexte
 * (un seul démarrage d'application et un seul cycle Liquibase drop/migrate
 * pour toute la suite).
 *
 * <p>Le jeu de données ({@code dataPostgres.sql}) est rechargé avant chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@Sql(scripts = "classpath:scripts/dataPostgres.sql")
public abstract class AbstractIntegrationTest {
}
