package com.barry.bank.api.controllers;

import com.barry.bank.api.AbstractIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Vérifie que la bean validation s'exécute réellement (issue #81) : sans
 * spring-boot-starter-validation sur le classpath, toutes les annotations
 * {@code @Valid}/{@code @Validated}/{@code @NotBlank}… étaient silencieusement
 * ignorées et les requêtes invalides atteignaient les services.
 */
class BeanValidationIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenDebitDescriptionIsBlank() {
        mockMvc.perform(post("/api/v1/accounts/{accountId}/debit", "cccc3333-0000-4000-b333-000000000003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50.00, "description": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value(containsString("description: Description must not be blank")));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenTransferAmountIsNegative() {
        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "cccc3333-0000-4000-b333-000000000003",
                                  "destinationAccountId": "aaaa1111-0000-4000-b111-000000000001",
                                  "amount": -5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value(containsString("amount: Amount must be greater than zero")));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WithAllInvalidFieldsOnCustomerCreation() {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value(containsString("email")))
                .andExpect(jsonPath("$.detail").value(containsString("firstName")))
                .andExpect(jsonPath("$.detail").value(containsString("lastName")));
    }

    @Test
    @SneakyThrows
    void shouldReturn400WhenPageSizeViolatesMin() {
        mockMvc.perform(get("/api/v1/customers")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value(containsString("size")));
    }
}