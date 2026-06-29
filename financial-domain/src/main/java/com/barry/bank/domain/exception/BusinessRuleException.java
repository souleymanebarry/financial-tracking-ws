package com.barry.bank.domain.exception;

/**
 * Raised when a request is well-formed but violates a business rule
 * (e.g. transferring to the same account). Maps to HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}