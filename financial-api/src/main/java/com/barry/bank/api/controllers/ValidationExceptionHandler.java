package com.barry.bank.api.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Réponses 400 de la bean validation sur {@code @Valid @RequestBody} (issue #81).
 *
 * <p>Advice dédié plutôt qu'un override de
 * {@code ResponseEntityExceptionHandler#handleMethodArgumentNotValid} dans
 * {@link GlobalExceptionHandler} : le détail par défaut de Spring
 * (« Invalid request content. ») ne cite pas les champs en faute, et un override
 * ne peut pas honorer proprement le contrat de nullité hérité (règle Sonar S2638).
 * {@code @Order(HIGHEST_PRECEDENCE)} garantit que cet advice est consulté avant
 * le mapping hérité de {@link GlobalExceptionHandler}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Log4j2
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        log.warn("Validation failed on {}: {}", request.getRequestURI(), detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}