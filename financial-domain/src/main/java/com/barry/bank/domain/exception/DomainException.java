package com.barry.bank.domain.exception;

/**
 * Base type for all business/domain exceptions raised by the model and the
 * application services. Kept free of any Spring or web dependency so it can
 * live in the domain module; the mapping to HTTP status codes is the
 * responsibility of the API layer.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}