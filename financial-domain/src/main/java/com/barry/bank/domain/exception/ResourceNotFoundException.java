package com.barry.bank.domain.exception;

/**
 * Raised when a requested entity does not exist. Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}