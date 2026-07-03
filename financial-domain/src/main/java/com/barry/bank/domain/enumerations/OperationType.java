package com.barry.bank.domain.enumerations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationType {

    DEBIT("Debit"),
    CREDIT("Credit");

    private final String label;
}
