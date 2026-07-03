package com.barry.bank.domain.enumerations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatus {

    CREATED("Created"),
    ACTIVATED("Activated"),
    SUSPENDED("Suspended");

    private final String label;
}
