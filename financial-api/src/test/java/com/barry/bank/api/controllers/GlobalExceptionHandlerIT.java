package com.barry.bank.api.controllers;

import com.barry.bank.api.AbstractIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Vérifie que les exceptions Spring MVC porteuses de statut (URL inconnue, méthode
 * non supportée, JSON illisible, media type, type mismatch) conservent leur statut
 * au lieu d'être converties en 500 par le fourre-tout {@code Exception.class} (issue #80).
 */
class GlobalExceptionHandlerIT extends AbstractIntegrationTest {

    private static final String MALFORMED_JSON = "{invalid";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @SneakyThrows
    void shouldReturn404ForUnknownUrl() {
        mockMvc.perform(get("/api/v1/inconnu")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @SneakyThrows
    void shouldReturn405ForUnsupportedMethod() {
        mockMvc.perform(put("/api/v1/customers"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @SneakyThrows
    void shouldReturn400ForMalformedJson() {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @SneakyThrows
    void shouldReturn415ForUnsupportedMediaType() {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @SneakyThrows
    void shouldReturn400ForPathVariableTypeMismatch() {
        mockMvc.perform(get("/api/v1/accounts/{accountId}", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}