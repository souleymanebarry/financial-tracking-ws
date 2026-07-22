package com.barry.bank.api.controllers.impl;

import com.barry.bank.api.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.barry.bank.api.controllers.testutils.TestUtils.printPrettyJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class CustomerControllerIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws SQLException {
        mockMvc = webAppContextSetup(webApplicationContext).build();
        if (jdbcTemplate.getDataSource() != null) {
          System.out.println("➡️ DB URL = "+ jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());
        }
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
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
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
    void shouldRejectPartialUpdateWhenEmailBelongsToAnotherCustomer() {
        // arrange — 'jane.smith@example.com' appartient déjà au client 22222222 (#94)
        String jsonBody = """
                {
                  "email": "jane.smith@example.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/customers/{customerId}", "33333333-cccc-4ccc-cccc-333333333333")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // l'email du client cible n'a pas changé
        List<Map<String, Object>> target = jdbcTemplate.queryForList(
                "SELECT email FROM customer WHERE customer_id = '33333333-cccc-4ccc-cccc-333333333333'");
        assertThat(target).hasSize(1);
        assertThat(target.get(0)).containsEntry("email", "alexandre.durant@google.com");

        // toujours un seul porteur de jane.smith@example.com
        List<Map<String, Object>> holders = jdbcTemplate.queryForList(
                "SELECT customer_id FROM customer WHERE email = 'jane.smith@example.com'");
        assertThat(holders).hasSize(1);
    }

    @Test
    @SneakyThrows
    void shouldAllowPartialUpdateWhenEmailUnchangedIgnoringCase() {
        // arrange — même email que l'actuel, casse différente : pas de faux conflit (#94)
        String jsonBody = """
                {
                  "firstName": "Alex",
                  "email": "ALEXANDRE.DURANT@GOOGLE.COM"
                }
                """;

        mockMvc.perform(patch("/api/v1/customers/{customerId}", "33333333-cccc-4ccc-cccc-333333333333")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alex"));
    }

    @Test
    @SneakyThrows
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldArchiveAndDeleteCustomerSuccessfully() {
        // Arrange – expected payload of the archiving microservice
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

        // 1 Archiving microservice stub to intercept the POST
        stubFor(WireMock.post(urlEqualTo("/api/v1/archives/customers"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*"))
                .withRequestBody(equalToJson(expectedPayload, true, true)) // ignore order, allow nulls
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                          "status": "ARCHIVED",
                          "message": "Customer archived successfully"
                        }
                    """)));

        // 2 Act – removal of the customer
        mockMvc.perform(delete("/api/v1/customers/{id}", "33333333-cccc-4ccc-cccc-333333333333"))
                .andExpect(status().isNoContent());

        // 3 Assert – check if the archive request has indeed been sent
        verify(postRequestedFor(urlEqualTo("/api/v1/archives/customers"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*"))
                .withRequestBody(equalToJson(expectedPayload, true, true)));

        // 4 check if the customer has indeed been deleted
        Integer countCustomer = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countCustomer).isZero();

        // 5 check that their accounts has indeed been deleted
        Integer countAccounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account WHERE customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countAccounts).isZero();

        // 6 check that their related operations has indeed been deleted
        Integer countOperations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation o " +
                        "JOIN account a ON o.account_id = a.account_id " +
                        "WHERE a.customer_id = '33333333-cccc-4ccc-cccc-333333333333'", Integer.class);
        assertThat(countOperations).isZero();

        System.out.println("Customer archived and deleted successfully with all related data.");
    }

}
