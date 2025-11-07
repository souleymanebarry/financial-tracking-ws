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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.barry.bank.financial.tracking_ws.testutils.TestUtils.printPrettyJson;

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

        // ✅ Vérification DB
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

        // ✅ Vérification DB
        List<Map<String, Object>> customers = jdbcTemplate.queryForList(
                "SELECT * FROM customer WHERE email = 'alexandre.durant@exemple.com'");
        assertThat(customers).hasSize(1);
    }

    @Test
    @SneakyThrows
    void shouldDeleteCustomerSuccessfully() {
        mockMvc.perform(delete("/api/v1/customers/{customerId}", "33333333-cccc-4ccc-cccc-333332313445"))
                .andExpect(status().isNoContent());

        // ✅ Vérification DB
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE customer_id = '33333333-cccc-4ccc-cccc-333332313445'", Integer.class);
        assertThat(count).isZero();
    }
}
