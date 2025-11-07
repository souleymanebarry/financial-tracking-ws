package com.barry.bank.financial.tracking_ws.controllers.impl;

import lombok.SneakyThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.barry.bank.financial.tracking_ws.testutils.TestUtils.printPrettyJson;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Sql(scripts = {"classpath:scripts/schemaPostgres.sql", "classpath:scripts/dataPostgres.sql"})
@AutoConfigureMockMvc
class BankAccountControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @SneakyThrows
    void shouldReturnAllAccountsSuccessfully() {

        mockMvc.perform(get("/api/v1/accounts/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rib").value("FR761234567890"))
                .andExpect(jsonPath("$[1].rib").value("FR769876543210"))
                .andExpect(jsonPath("$[2].rib").value("FR761112223334"))
                .andExpect(jsonPath("$.length()").value(3));

    }

    @Test
    @SneakyThrows
    void shouldReturnAccountByIdSuccessfully() {

        MvcResult result = mockMvc.perform(get("/api/v1/accounts/{accountId}", "aaaa1111-0000-4000-b111-000000000001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("aaaa1111-0000-4000-b111-000000000001"))
                .andExpect(jsonPath("$.rib").value("FR761234567890"))
                .andExpect(jsonPath("$.balance").value(2500.00))
                .andExpect(jsonPath("$.accountType").value("CURRENT ACCOUNT"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        printPrettyJson(result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    void shouldReturnAccountsWithPagination() {

        mockMvc.perform(get("/api/v1/accounts")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].rib").value("FR761234567890"))
                .andExpect(jsonPath("$[0].balance").value(2500.00))
                .andExpect(jsonPath("$[1].rib").value("FR769876543210"))
                .andExpect(jsonPath("$[1].balance").value(1500.00));
    }

    @Test
    @SneakyThrows
    void shouldGetAccountOperationsSuccessfully() {

        MvcResult result =  mockMvc.perform(get("/api/v1/accounts/{accountId}/operations","cccc3333-0000-4000-b333-000000000003")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].operationId").value("33333333-0000-4000-b333-000000000003"))
                .andExpect(jsonPath("$[0].operationAmount").value(150.00))
                .andExpect(jsonPath("$[0].operationType").value("DEBIT"))
                .andExpect(jsonPath("$[0].operationNumber").value("OP-20231010-000003"))
                .andExpect(jsonPath("$[1].operationId").value("44444444-0000-4000-b444-000000000004"))
                .andExpect(jsonPath("$[1].operationAmount").value( 1200.00))
                .andExpect(jsonPath("$[1].operationType").value("CREDIT"))
                .andExpect(jsonPath("$[1].description").value("Project freelance payment"))
                .andReturn();

        printPrettyJson(result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    void shouldGetAccountHistoryWithPagination() {
      mockMvc.perform(get("/api/v1/accounts/{accountId}/history","cccc3333-0000-4000-b333-000000000003")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("cccc3333-0000-4000-b333-000000000003"))
                .andExpect(jsonPath("$.accountHolderName").value("Durant"))
                .andExpect(jsonPath("$.balance").value(3200.00))
                .andExpect(jsonPath("$.operations").isArray())
                .andExpect(jsonPath("$.operations[0].operationId").value("33333333-0000-4000-b333-000000000003"))
                .andExpect(jsonPath("$.operations[0].operationAmount").value(150.00))
                .andExpect(jsonPath("$.operations[0].operationType").value("DEBIT"))
                .andExpect(jsonPath("$.operations[0].operationNumber").value("OP-20231010-000003"))
                .andExpect(jsonPath("$.operations[1].operationId").value("44444444-0000-4000-b444-000000000004"))
                .andExpect(jsonPath("$.operations[1].operationAmount").value( 1200.00))
                .andExpect(jsonPath("$.operations[1].operationType").value("CREDIT"))
                .andExpect(jsonPath("$.operations[1].description").value("Project freelance payment"));
    }

    @Test
    @SneakyThrows
    void shouldDebitAccountAndPersistOperation() {
       String JsonBody = """
                {
                  "amount": 200,
                  "description": "Withdrawal at the counter"
                }
                """;
        mockMvc.perform(post("/api/v1/accounts/{accountId}/debit","cccc3333-0000-4000-b333-000000000003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonBody))
                .andExpect(status().isOk());

        // Vérifier en base
        BigDecimal newBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_id = 'cccc3333-0000-4000-b333-000000000003'",
                BigDecimal.class);

        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(3000.00));

        List<Map<String, Object>> operations =
                jdbcTemplate.queryForList("SELECT * FROM operation WHERE description = 'Withdrawal at the counter'");

        assertAll(
                () -> assertThat(operations).hasSize(1),
                () -> assertThat(operations.get(0))
                        .containsEntry("account_id", UUID.fromString("cccc3333-0000-4000-b333-000000000003"))
                        .containsEntry("operation_type", "DEBIT")
                        .containsEntry("operation_amount", new BigDecimal("200.00"))
        );
    }

    @Test
    @SneakyThrows
    void shouldCreditAccountAndPersistOperation() {
        String JsonBody = """
                {
                  "amount": 1000,
                  "description": "Bonus credit"
                }
                """;
        mockMvc.perform(post("/api/v1/accounts/{accountId}/credit","bbbb2222-0000-4000-b222-000000000002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonBody))
                .andExpect(status().isOk());

        // Vérifier en base
        BigDecimal newBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_id = 'bbbb2222-0000-4000-b222-000000000002'",
                BigDecimal.class);

        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(2500.00));

        List<Map<String, Object>> operations =
                jdbcTemplate.queryForList("SELECT * FROM operation WHERE description = 'Bonus credit'");

        assertAll(
                () -> assertThat(operations).hasSize(1),
                () -> assertThat(operations.get(0))
                        .containsEntry("account_id", UUID.fromString("bbbb2222-0000-4000-b222-000000000002"))
                        .containsEntry("operation_type", "CREDIT")
                        .containsEntry("operation_amount", new BigDecimal("1000.00"))
        );
    }

    @Test
    @SneakyThrows
    void shouldTransferAmountBetweenAccountsAndPersistOperation() {
        String JsonBody = """
                {
                  "sourceAccountId": "cccc3333-0000-4000-b333-000000000003",
                  "destinationAccountId": "bbbb2222-0000-4000-b222-000000000002",
                  "amount": 200
                }
                """;
        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonBody))
                .andExpect(status().isOk());

        // Vérifier en base
        BigDecimal sourceBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_id = 'cccc3333-0000-4000-b333-000000000003'",
                BigDecimal.class);

        BigDecimal destinationBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_id = 'bbbb2222-0000-4000-b222-000000000002'",
                BigDecimal.class);

        assertAll(
                () -> assertThat(sourceBalance).isEqualByComparingTo(BigDecimal.valueOf(3000.00)), // 3200.00 - 200
                () -> assertThat(destinationBalance).isEqualByComparingTo(BigDecimal.valueOf(1700.00)) // 1500 + 200
        );


        // check record operations
        Integer debitOps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation WHERE account_id = 'cccc3333-0000-4000-b333-000000000003' AND operation_type = 'DEBIT'",
                Integer.class);

        Integer creditOps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM operation WHERE account_id = 'bbbb2222-0000-4000-b222-000000000002' AND operation_type = 'CREDIT'",
                Integer.class);

        assertAll(
                () -> assertThat(debitOps).isEqualTo(2),
                () -> assertThat(creditOps).isEqualTo(1)
        );
    }

    @Test
    @SneakyThrows
    void shouldCreateCurrentAccountSuccessfully() {
        // 🧠 GIVEN
        String JsonBody = """
                {
                  "customerDTO": {
                      "customerId": "33333333-cccc-4ccc-cccc-333332313445"
                  },
                  "rib": "FR769999888877",
                  "balance": 500.00,
                  "accountType": "CURRENT ACCOUNT",
                  "overDraft": 200,
                  "interestRate": null
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/accounts/{customerId}/current-account", "33333333-cccc-4ccc-cccc-333332313445")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.accountId").exists())
                .andExpect(jsonPath("$.balance").value(BigDecimal.valueOf(500.00)))
                .andExpect(jsonPath("$.accountType").value("CURRENT ACCOUNT"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        printPrettyJson(result.getResponse().getContentAsString());

        List<Map<String, Object>> accounts = jdbcTemplate.queryForList(
                "SELECT * FROM account WHERE rib = 'FR769999888877'");

        assertAll(
                () -> assertThat(accounts).hasSize(1),
                () -> assertThat(accounts.get(0))
                        .containsEntry("balance", new BigDecimal("500.00"))
                        .containsEntry("account_type", "CURRENT ACCOUNT")
                        .containsEntry("status", "CREATED")
                        .containsEntry("rib", "FR769999888877")
        );
    }

    @Test
    @SneakyThrows
    void shouldCreateSavingAccountSuccessfully() {
        // GIVEN
        String JsonBody = """
                {
                  "customerDTO": {
                      "customerId": "33333333-cccc-4ccc-cccc-333332313445"
                  },
                  "rib": "FR769999882222",
                  "balance": 1500.00,
                  "accountType": "SAVING ACCOUNT",
                  "overDraft": null,
                  "interestRate": 2.7
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/accounts/{customerId}/saving-account", "33333333-cccc-4ccc-cccc-333332313445")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.accountId").exists())
                .andExpect(jsonPath("$.balance").value(BigDecimal.valueOf(1500.00)))
                .andExpect(jsonPath("$.accountType").value("SAVING ACCOUNT"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        printPrettyJson(result.getResponse().getContentAsString());

        List<Map<String, Object>> accounts = jdbcTemplate.queryForList(
                "SELECT * FROM account WHERE rib = 'FR769999882222'");

        assertAll(
                () -> assertThat(accounts).hasSize(1),
                () -> assertThat(accounts.get(0))
                        .containsEntry("balance", new BigDecimal("1500.00"))
                        .containsEntry("account_type", "SAVING ACCOUNT")
                        .containsEntry("status", "CREATED")
                        .containsEntry("rib", "FR769999882222")
        );
    }
}
