package com.barry.bank.domain.exception;

/**
 * Raised when creating an entity would violate a uniqueness constraint
 * (e.g. an already used email). Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}