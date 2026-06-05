package com.barry.bank.batch.statement.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;

import java.time.format.DateTimeFormatter;

/**
 * Constantes de mise en page du relevé PDF — couleurs, polices, espacements.
 */
public final class PdfStatementStyle {

    private PdfStatementStyle() {}

    // Colors
    public static final DeviceRgb HEADER_BG    = new DeviceRgb(0, 51, 102);
    public static final DeviceRgb HEADER_FG    = new DeviceRgb(255, 255, 255);
    public static final DeviceRgb TABLE_HDR_BG = new DeviceRgb(232, 240, 254);
    public static final DeviceRgb ROW_ALT_BG   = new DeviceRgb(249, 249, 249);
    public static final DeviceRgb BORDER       = new DeviceRgb(200, 200, 200);
    public static final DeviceRgb DEBIT_COLOR  = new DeviceRgb(180, 0, 0);
    public static final DeviceRgb CREDIT_COLOR = new DeviceRgb(0, 120, 0);
    public static final DeviceRgb MUTED        = new DeviceRgb(120, 120, 120);

    // Date formatters (thread-safe)
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Tailles de police (points PDF)
    public static final float FONT_TITLE   = 16f;
    public static final float FONT_HEADER  = 11f;
    public static final float FONT_LABEL   =  9f;
    public static final float FONT_BODY    =  8f;
    public static final float FONT_FOOTER  =  7f;
    public static final float FONT_BALANCE = 14f;

    // Padding des cellules
    public static final float PAD_HEADER  = 12f;
    public static final float PAD_BALANCE = 10f;
    public static final float PAD_CELL    =  5f;
    public static final float PAD_SMALL   =  4f;

    // Mise en page
    public static final float BORDER_WIDTH  = 0.5f;
    public static final float MARGIN_TOP    = 36f;
    public static final float MARGIN_SIDE   = 36f;
    public static final float MARGIN_BOTTOM = 54f;
    public static final float FOOTER_MARGIN = 20f;
}
