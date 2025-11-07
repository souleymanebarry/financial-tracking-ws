package com.barry.bank.financial.tracking_ws.enums;

public enum AccountStatus {

    CREATED("Created"),
    ACTIVATED("Activated"),
    SUSPENDED("Suspended");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
