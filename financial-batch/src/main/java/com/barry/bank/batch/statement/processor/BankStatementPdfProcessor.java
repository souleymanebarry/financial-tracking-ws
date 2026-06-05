package com.barry.bank.batch.statement.processor;

import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.StatementLine;
import com.barry.bank.domain.entities.enums.OperationType;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.barry.bank.batch.statement.pdf.PdfStatementStyle.*;

/**
 * Génère le PDF d'un {@link BankStatement} via iText 7.
 *
 * <p>Structure du document :
 * <ol>
 *   <li>En-tête (fond bleu marine) — titre + période</li>
 *   <li>Informations titulaire — nom, email, RIB</li>
 *   <li>Résumé des soldes — ouverture / clôture</li>
 *   <li>Tableau des opérations — Date | N° | Libellé | Débit | Crédit | Solde</li>
 *   <li>Mention légale</li>
 * </ol>
 */
@Component
@Log4j2
public class BankStatementPdfProcessor implements ItemProcessor<BankStatement, StatementPdfResult> {

    // NumberFormat n'est pas thread-safe — ThreadLocal évite la corruption des montants
    // en cas d'exécutions parallèles. remove() est appelé après chaque génération.
    private static final ThreadLocal<NumberFormat> AMOUNT_FMT = ThreadLocal.withInitial(() -> {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.FRANCE);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        return fmt;
    });

    @Override
    public StatementPdfResult process(@NonNull BankStatement statement) throws IOException {
        log.debug("Génération PDF — relevé: {}, compte: {}",
                statement.getId(), statement.getAccount().getAccountId());
        return new StatementPdfResult(statement, generatePdf(statement));
    }

    private byte[] generatePdf(BankStatement statement) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document document = new Document(pdf, PageSize.A4)) {

            document.setMargins(MARGIN_TOP, MARGIN_SIDE, MARGIN_BOTTOM, MARGIN_SIDE);

            addHeader(document, statement, bold, regular);
            addSeparator(document);
            addAccountInfo(document, statement, bold, regular);
            addSeparator(document);
            addBalanceSummary(document, statement, bold, regular);
            addSeparator(document);
            addOperationsTable(document, statement, bold, regular);
            addLegalFooter(document, regular);
        } finally {
            AMOUNT_FMT.remove();
        }

        return baos.toByteArray();
    }

    private void addHeader(Document doc, BankStatement stmt, PdfFont bold, PdfFont regular) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        header.addCell(new Cell()
                .setBackgroundColor(HEADER_BG).setBorder(null).setPadding(PAD_HEADER)
                .add(new Paragraph("RELEVÉ DE COMPTE").setFont(bold).setFontSize(FONT_TITLE).setFontColor(HEADER_FG)));

        String period = stmt.getPeriodStart().format(DATE_FMT) + " – " + stmt.getPeriodEnd().format(DATE_FMT);
        header.addCell(new Cell()
                .setBackgroundColor(HEADER_BG).setBorder(null).setPadding(PAD_HEADER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Période").setFont(regular).setFontSize(FONT_LABEL).setFontColor(HEADER_FG))
                .add(new Paragraph(period).setFont(bold).setFontSize(FONT_HEADER).setFontColor(HEADER_FG)));

        doc.add(header);
    }

    private void addAccountInfo(Document doc, BankStatement stmt, PdfFont bold, PdfFont regular) {
        var account  = stmt.getAccount();
        var customer = account.getCustomer();
        SolidBorder border = new SolidBorder(BORDER, BORDER_WIDTH);

        Table info = new Table(UnitValue.createPercentArray(new float[]{25, 75})).useAllAvailableWidth();

        addInfoRow(info, "Titulaire", customer.getFirstName() + " " + customer.getLastName(), bold, regular, border);
        addInfoRow(info, "Email",     Objects.requireNonNullElse(customer.getEmail(), ""),     bold, regular, border);
        addInfoRow(info, "RIB",       Objects.requireNonNullElse(account.getRib(), ""),        bold, regular, border);
        addInfoRow(info, "Généré le", stmt.getGeneratedAt().format(DT_FMT),                    bold, regular, border);

        doc.add(info);
    }

    private void addInfoRow(Table table, String label, String value,
                            PdfFont bold, PdfFont regular, SolidBorder border) {
        table.addCell(new Cell().setBorderTop(border).setBorderBottom(border).setBorderLeft(null).setBorderRight(null)
                .setPadding(PAD_CELL).add(new Paragraph(label).setFont(bold).setFontSize(FONT_LABEL)));
        table.addCell(new Cell().setBorderTop(border).setBorderBottom(border).setBorderLeft(null).setBorderRight(null)
                .setPadding(PAD_CELL).add(new Paragraph(value).setFont(regular).setFontSize(FONT_LABEL)));
    }

    private void addBalanceSummary(Document doc, BankStatement stmt, PdfFont bold, PdfFont regular) {
        Table summary = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        summary.addCell(buildBalanceCell("Solde d'ouverture", stmt.getOpeningBalance(), bold, regular));
        summary.addCell(buildBalanceCell("Solde de clôture",  stmt.getClosingBalance(), bold, regular));
        doc.add(summary);
    }

    private Cell buildBalanceCell(String label, BigDecimal amount, PdfFont bold, PdfFont regular) {
        return new Cell()
                .setBackgroundColor(TABLE_HDR_BG)
                .setBorder(new SolidBorder(BORDER, BORDER_WIDTH))
                .setPadding(PAD_BALANCE)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(label).setFont(regular).setFontSize(FONT_LABEL))
                .add(new Paragraph(formatAmount(amount)).setFont(bold).setFontSize(FONT_BALANCE));
    }

    private void addOperationsTable(Document doc, BankStatement stmt, PdfFont bold, PdfFont regular) {
        doc.add(new Paragraph("Détail des opérations").setFont(bold).setFontSize(FONT_HEADER).setMarginTop(PAD_SMALL));

        Table table = new Table(UnitValue.createPercentArray(new float[]{13, 16, 36, 12, 12, 11}))
                .useAllAvailableWidth();

        for (String col : new String[]{"Date", "N° Opération", "Libellé", "Débit", "Crédit", "Solde"}) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(TABLE_HDR_BG)
                    .setBorder(new SolidBorder(BORDER, BORDER_WIDTH))
                    .setPadding(PAD_CELL)
                    .add(new Paragraph(col).setFont(bold).setFontSize(FONT_BODY).setTextAlignment(TextAlignment.CENTER)));
        }

        List<StatementLine> lines = stmt.getLines().stream()
                .sorted(Comparator.comparing(StatementLine::getOperationDate))
                .toList();

        int rowIndex = 0;
        for (StatementLine line : lines) {
            DeviceRgb rowBg  = (rowIndex++ % 2 == 0) ? null : ROW_ALT_BG;
            boolean   isDebit = OperationType.DEBIT.equals(line.getOperationType());

            table.addCell(dataCell(line.getOperationDate().format(DATE_FMT),                      regular, rowBg, TextAlignment.CENTER));
            table.addCell(dataCell(Objects.requireNonNullElse(line.getOperationNumber(), ""),      regular, rowBg, TextAlignment.LEFT));
            table.addCell(dataCell(Objects.requireNonNullElse(line.getLabel(), ""),               regular, rowBg, TextAlignment.LEFT));
            table.addCell(coloredAmountCell(isDebit  ? formatAmount(line.getAmount()) : "",       DEBIT_COLOR,  regular, rowBg));
            table.addCell(coloredAmountCell(!isDebit ? formatAmount(line.getAmount()) : "",       CREDIT_COLOR, regular, rowBg));
            table.addCell(coloredAmountCell(formatAmount(line.getRunningBalance()),                null, bold, rowBg));
        }

        doc.add(table);
    }

    private void addLegalFooter(Document doc, PdfFont regular) {
        doc.add(new Paragraph(
                "Ce relevé est un document officiel. Toute contestation doit être signalée dans un délai de 30 jours. " +
                "Conservez ce document conformément aux obligations légales (durée minimale 5 ans).")
                .setFont(regular).setFontSize(FONT_FOOTER).setFontColor(MUTED).setMarginTop(FOOTER_MARGIN));
    }

    private Cell dataCell(String text, PdfFont font, DeviceRgb bg, TextAlignment align) {
        Cell cell = new Cell().setBorder(new SolidBorder(BORDER, BORDER_WIDTH)).setPadding(PAD_SMALL)
                .add(new Paragraph(text).setFont(font).setFontSize(FONT_BODY).setTextAlignment(align));
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private Cell coloredAmountCell(String text, DeviceRgb color, PdfFont font, DeviceRgb bg) {
        Paragraph p = new Paragraph(text).setFont(font).setFontSize(FONT_BODY).setTextAlignment(TextAlignment.RIGHT);
        if (color != null && !text.isEmpty()) p.setFontColor(color);
        Cell cell = new Cell().setBorder(new SolidBorder(BORDER, BORDER_WIDTH)).setPadding(PAD_SMALL).add(p);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }

    private void addSeparator(Document doc) {
        doc.add(new Paragraph(" ").setFontSize(PAD_SMALL));
    }

    private String formatAmount(BigDecimal amount) {
        return AMOUNT_FMT.get().format(amount == null ? BigDecimal.ZERO : amount.abs()) + " €";
    }
}
