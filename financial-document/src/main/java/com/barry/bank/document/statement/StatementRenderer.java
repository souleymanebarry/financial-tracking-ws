package com.barry.bank.document.statement;

import java.util.Locale;

public interface StatementRenderer {

    byte[] render(StatementData data, Locale locale);
}