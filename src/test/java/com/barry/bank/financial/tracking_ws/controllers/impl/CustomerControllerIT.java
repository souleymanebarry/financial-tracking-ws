package com.barry.bank.financial.tracking_ws.controllers.impl;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.barry.bank.financial.tracking_ws.testutils.TestUtils.printPrettyJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Sql(scripts = {"classpath:scripts/schemaPostgres.sql", "classpath:scripts/dataPostgres.sql"})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class CustomerControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws SQLException {
        mockMvc = webAppContextSetup(webApplicationContext).build();
        System.out.println("➡️ DB URL = "+ jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());
    }

    @Test
    @SneakyThrows
    void shouldReturnAllCustomers() {

        mockMvc.perform(get("/api/v1/customers/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                .andExpect(jsonPath("$[2].firstName").value("Alexandre"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$[1].email").value("jane.smith@example.com"))
                .andExpect(jsonPath("$[2].email").value("alexandre.durant@google.com"))
                .andExpect(jsonPath("$[0].gender").value("MALE"))
                .andExpect(jsonPath("$[1].gender").value("FEMALE"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @SneakyThrows
    void shouldReturnCustomerById() {
        MvcResult result = mockMvc.perform(get("/api/v1/customers/{id}", "11111111-aaaa-4aaa-aaaa-111111111111")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andReturn();

        printPrettyJson(result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    void shouldReturnCustomersWithPagination() {
        mockMvc.perform(get("/api/v1/customers")
                        .param("page", "0")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[1].customerId").value("22222222-bbbb-4bbb-bbbb-222222222222"));
    }

    @Test
    @SneakyThrows
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldCreateCustomerSuccessfully() {
        // arrange
        String jsonBody = """
                {
                  "firstName": "Emily",
                  "lastName": "BROWN",
                  "email": "emily.brown-ext@google.com",
                  "gender": "FEMALE"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Emily"))
                .andExpect(jsonPath("$.email").value("emily.brown-ext@google.com"));

        // Vérification DB
        List<Map<String, Object>> customers = jdbcTemplate.queryForList(
                "SELECT * FROM customer WHERE email = 'emily.brown-ext@google.com'");
        assertThat(customers).hasSize(1);
    }

    @Test
    @SneakyThrows
    void shouldPartiallyUpdateCustomerSuccessfully() {
        // arrange
        String jsonBody = """
                {
                  "email": "alexandre.durant@exemple.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/customers/{customerId}", "33333333-cccc-4ccc-cccc-333333333333")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alexandre"))
                .andExpect(jsonPath("$.email").value("alexandre.durant@exemple.com"));

        // Vérification DB
        List<Map<String, Object>> customers = jdbcTemplate.queryForList(
                "SELECT * FROM customer WHERE email = 'alexandre.durant@exemple.com'");
        assertThat(customers).hasSize(1);
    }

    @Test
    @SneakyThrows
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldArchiveAndDeleteCustomerSuccessfully() {
        // Arrange – payload attendu du microservice d’archivage
        String expectedPayload = """
                    {
                      "customerId": "33333333-cccc-4ccc-cccc-333333333333",
                      "firstName": "Alexandre",
                      "lastName": "Durant",
                      "email": "alexandre.durant@google.com",
                      "gender": "MALE",
                      "accounts": [
                        {
                          "accountId": "cccc3333-0000-4000-b333-000000000003",
                          "rib": "FR761112223334",
                          "balance": 3200.00,
                          "accountType": "CURRENT ACCOUNT",
                          "overDraft": 400.00,
                          "interestRate": null,
                          "status": "CREATED",
                          "createdAt": "2025-11-09T03:06:56",
                          "operations": [
                            {
                              "operationId": "33333333-0000-4000-b333-000000000003",
                              "operationNumber": "OP-20231010-000003",
                              "operationAmount": 150.00,
                              "operationDate": "2025-11-09T03:06:56",
                              "operationType": "DEBIT",
                              "description": "Restaurant payment"
                            },
                            {
                              "operationId": "44444444-0000-4000-b444-000000000004",
                              "operationNumber": "OP-20231010-000004",
                              "operationAmount": 1200.00,
                              "operationDate": "2025-11-09T03:06:56",
                              "operationType": "CREDIT",
                              "description": "Project freelance payment"
                            }
                          ]
                        }
                      ]
                    }
                    """;

        // 1 Stub du microservice d’archivage pour intercepter le POST
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/archives/customers"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(expectedPayload, true, true)) // ignore order, allow nulls
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                          "status": "ARCHIVED",
                          "message": "Customer archived successfully"
                        }
                    """)));

        // 2 Act – suppression du client (le service enverra le POST d’archivage)
        mockMvc.perform(delete("/api/v1/customers/{id}", "33333333-cccc-4ccc-cccc-333333333333"))
                .andExpect(status().isNoContent());

        // 3 Assert – vérifie que la requête d’archivage a bien été envoyée
        verify(postRequestedFor(urlEqualTo("/api/v1/archives/customers"))
                .withRequestBody(equalToJson(expectedPayload, true, true)));

        // 4 Vérifie que le client a bien été supprimé
        Integer countCustomer = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countCustomer).isZero();

        // 5 Vérifie que ses comptes ont bien été supprimés
        Integer countAccounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account WHERE customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countAccounts).isZero();

        // 6 Vérifie que ses opérations liées ont bien été supprimées
        Integer countOperations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation o " +
                        "JOIN account a ON o.account_id = a.account_id " +
                        "WHERE a.customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countOperations).isZero();

        System.out.println("Customer archived and deleted successfully with all related data.");
    }

}
