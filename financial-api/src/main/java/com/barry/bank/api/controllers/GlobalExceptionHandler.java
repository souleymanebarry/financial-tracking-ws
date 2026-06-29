package com.barry.bank.api.controllers;

import com.barry.bank.api.exception.ArchiveServiceException;
import com.barry.bank.domain.exception.BusinessRuleException;
import com.barry.bank.domain.exception.DuplicateResourceException;
import com.barry.bank.domain.exception.InsufficientBalanceException;
import com.barry.bank.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation on {}: {}", request.getRequestURI(), detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler({InsufficientBalanceException.class, BusinessRuleException.class, IllegalStateException.class})
    public ProblemDetail handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        log.warn("Business rule violation on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(ArchiveServiceException.class)
    public ProblemDetail handleArchiveService(ArchiveServiceException ex, HttpServletRequest request) {
        log.error("Archive service failure on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Archive Service Unavailable");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}

