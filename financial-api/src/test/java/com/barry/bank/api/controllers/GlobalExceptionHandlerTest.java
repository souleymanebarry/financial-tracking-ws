package com.barry.bank.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturn500WithGenericMessageForUnexpectedException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");

        ProblemDetail problem = handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    }
}