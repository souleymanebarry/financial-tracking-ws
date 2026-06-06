package com.barry.bank.document.fop;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class AmountFormatter {

    private final NumberFormat fmt;

    public AmountFormatter(Locale locale) {
        fmt = NumberFormat.getNumberInstance(locale);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        if (fmt instanceof DecimalFormat df) {
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setGroupingSeparator(' ');
            df.setDecimalFormatSymbols(symbols);
        }
    }

    public String format(BigDecimal amount) {
        BigDecimal value = (amount == null) ? BigDecimal.ZERO : amount.abs();
        return fmt.format(value) + " €";
    }
}
