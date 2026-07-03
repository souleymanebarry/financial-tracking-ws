package com.barry.bank.batch.statement.pdf;

import com.barry.bank.domain.model.BankStatement;

/**
 * Résultat du processor PDF : le relevé enrichi + les bytes du document généré.
 * Transporté du processor vers le writer dans le chunk.
 */
public record StatementPdfResult(BankStatement statement, byte[] pdfBytes) {}
