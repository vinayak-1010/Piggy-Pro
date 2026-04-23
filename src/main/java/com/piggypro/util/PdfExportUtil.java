package com.piggypro.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PdfExportUtil.java
 * ─────────────────────────────────────────────────────
 * Generates formatted PDF expense reports using iText 5.
 *
 * Usage:
 *   String path = PdfExportUtil.exportCategoryReport(
 *       userId, "Vinayak", catMap, from, to, totalExp, totalInc);
 *
 * Output saved to: ~/PiggyPro/exports/Report_YYYY-MM-DD.pdf
 */
public class PdfExportUtil {

    // ── Colors ─────────────────────────────────────
    private static final BaseColor ACCENT      = new BaseColor(37, 99, 235);
    private static final BaseColor ACCENT_LIGHT = new BaseColor(219, 234, 254);
    private static final BaseColor DARK        = new BaseColor(15, 23, 42);
    private static final BaseColor MID         = new BaseColor(71, 85, 105);
    private static final BaseColor LIGHT       = new BaseColor(241, 245, 251);
    private static final BaseColor WHITE       = BaseColor.WHITE;
    private static final BaseColor RED         = new BaseColor(239, 68, 68);
    private static final BaseColor GREEN       = new BaseColor(16, 185, 129);

    // ── Fonts ──────────────────────────────────────
    private static Font titleFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, DARK); }
    private static Font subtitleFont() { return FontFactory.getFont(FontFactory.HELVETICA, 11, MID); }
    private static Font headingFont()  { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, WHITE); }
    private static Font bodyFont()     { return FontFactory.getFont(FontFactory.HELVETICA, 11, DARK); }
    private static Font bodyBoldFont() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK); }
    private static Font smallFont()    { return FontFactory.getFont(FontFactory.HELVETICA, 9, MID); }
    private static Font redFont()      { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, RED); }
    private static Font greenFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GREEN); }

    // ── Output directory ───────────────────────────
    private static final String EXPORT_DIR = System.getProperty("user.home")
            + File.separator + "PiggyPro" + File.separator + "exports";

    // ══════════════════════════════════════════════
    // PUBLIC ENTRY POINTS
    // ══════════════════════════════════════════════

    /**
     * Exports a category-wise spending report to PDF.
     *
     * @param username    logged-in user's name for the report header
     * @param categoryMap map of category → total amount
     * @param from        report start date
     * @param to          report end date
     * @param totalExp    total expenses in range
     * @param totalInc    total income in range
     * @return absolute path to the generated file
     */
    public static String exportCategoryReport(
            String username,
            Map<String, Double> categoryMap,
            LocalDate from, LocalDate to,
            double totalExp, double totalInc) throws Exception {

        String filename = "Category_Report_" + from + "_to_" + to + ".pdf";
        String path = ensureDir() + File.separator + filename;

        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        addHeader(doc, "Category Spending Report", username, from, to);
        addSummaryBar(doc, totalExp, totalInc);

        // Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.5f, 2f, 1.5f});
        table.setSpacingBefore(16);

        addTableHeader(table, new String[]{"Category", "Transactions", "Total Spent", "% of Total"});

        double grand = categoryMap.values().stream().mapToDouble(Double::doubleValue).sum();
        int rowNum = 0;
        for (Map.Entry<String, Double> e : categoryMap.entrySet()) {
            double pct = grand > 0 ? e.getValue() / grand * 100 : 0;
            BaseColor bg = rowNum % 2 == 0 ? WHITE : LIGHT;
            addTableRow(table, bg,
                    e.getKey(),
                    "—",
                    "Rs. " + fmt(e.getValue()),
                    String.format("%.1f%%", pct));
            rowNum++;
        }

        // Totals row
        addTotalRow(table, "TOTAL", "", "Rs. " + fmt(grand), "100.0%");
        doc.add(table);

        addFooter(doc);
        doc.close();
        return path;
    }

    /**
     * Exports a monthly spending report to PDF.
     *
     * @param username     logged-in user's name
     * @param monthlyData  map of "YYYY-MM" → expense amount
     * @param incomeData   map of "YYYY-MM" → income amount
     */
    public static String exportMonthlyReport(
            String username,
            Map<String, Double> monthlyData,
            Map<String, Double> incomeData,
            LocalDate from, LocalDate to) throws Exception {

        String filename = "Monthly_Report_" + from + "_to_" + to + ".pdf";
        String path = ensureDir() + File.separator + filename;

        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        addHeader(doc, "Monthly Spending Report", username, from, to);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 2f, 2f});
        table.setSpacingBefore(16);

        addTableHeader(table, new String[]{"Month", "Transactions", "Expenses", "Income"});

        int rowNum = 0;
        for (Map.Entry<String, Double> e : monthlyData.entrySet()) {
            BaseColor bg = rowNum % 2 == 0 ? WHITE : LIGHT;
            double inc = incomeData.getOrDefault(e.getKey(), 0.0);
            addTableRow(table, bg,
                    formatMonth(e.getKey()),
                    "—",
                    "Rs. " + fmt(e.getValue()),
                    "Rs. " + fmt(inc));
            rowNum++;
        }

        doc.add(table);
        addFooter(doc);
        doc.close();
        return path;
    }

    /**
     * Exports all individual transactions to PDF.
     *
     * @param username  logged-in user's name
     * @param expenses  list of expense records as Object arrays:
     *                  [date(String), description, category, amount(double), type]
     */
    public static String exportAllTransactions(
            String username,
            List<Object[]> expenses,
            LocalDate from, LocalDate to,
            double totalExp, double totalInc) throws Exception {

        String filename = "Transactions_" + from + "_to_" + to + ".pdf";
        String path = ensureDir() + File.separator + filename;

        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        addHeader(doc, "All Transactions Report", username, from, to);
        addSummaryBar(doc, totalExp, totalInc);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 3f, 2f, 1.5f, 1.2f});
        table.setSpacingBefore(16);

        addTableHeader(table, new String[]{"Date", "Description", "Category", "Amount", "Type"});

        int rowNum = 0;
        for (Object[] row : expenses) {
            BaseColor bg = rowNum % 2 == 0 ? WHITE : LIGHT;
            boolean isIncome = "Income".equals(row[4]);
            PdfPCell amtCell = new PdfPCell(new Phrase(
                    (isIncome ? "+" : "-") + "Rs." + fmt((Double) row[3]),
                    isIncome ? greenFont() : redFont()));
            amtCell.setBorder(Rectangle.NO_BORDER);
            amtCell.setBackgroundColor(bg);
            amtCell.setPadding(8);

            addPartialRow(table, bg,
                    (String) row[0], (String) row[1], (String) row[2]);
            table.addCell(amtCell);
            PdfPCell typeCell = styledCell((String) row[4], bodyFont(), bg);
            table.addCell(typeCell);
            rowNum++;
        }

        doc.add(table);
        addFooter(doc);
        doc.close();
        return path;
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════

    private static void addHeader(Document doc, String title,
                                  String username, LocalDate from, LocalDate to)
            throws Exception {
        // Title
        Paragraph t = new Paragraph(title, titleFont());
        t.setAlignment(Element.ALIGN_LEFT);
        t.setSpacingAfter(4);
        doc.add(t);

        // Subtitle
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Paragraph sub = new Paragraph(
                "Prepared for: " + username + "   |   Period: "
                        + from.format(fmt) + " – " + to.format(fmt),
                subtitleFont());
        sub.setSpacingAfter(16);
        doc.add(sub);

        // Divider
        PdfPTable div = new PdfPTable(1);
        div.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setBorder(Rectangle.NO_BORDER);
        line.setBackgroundColor(ACCENT);
        line.setFixedHeight(3f);
        div.addCell(line);
        div.setSpacingAfter(16);
        doc.add(div);
    }

    private static void addSummaryBar(Document doc,
                                      double totalExp, double totalInc)
            throws Exception {
        PdfPTable bar = new PdfPTable(3);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[]{1f, 1f, 1f});
        bar.setSpacingAfter(16);

        addSummaryCell(bar, "Total Expenses", "Rs. " + fmt(totalExp), ACCENT_LIGHT, redFont());
        addSummaryCell(bar, "Total Income",   "Rs. " + fmt(totalInc), LIGHT,        greenFont());
        addSummaryCell(bar, "Net Savings",
                "Rs. " + fmt(totalInc - totalExp), LIGHT, bodyBoldFont());
        doc.add(bar);
    }

    private static void addSummaryCell(PdfPTable table, String label,
                                       String value, BaseColor bg, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(228, 232, 242));
        cell.setBackgroundColor(bg);
        cell.setPadding(12);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", smallFont()));
        p.add(new Chunk(value, valFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private static void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headingFont()));
            cell.setBackgroundColor(ACCENT);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(10);
            table.addCell(cell);
        }
    }

    private static void addTableRow(PdfPTable table, BaseColor bg,
                                    String... values) {
        for (String v : values) {
            table.addCell(styledCell(v, bodyFont(), bg));
        }
    }

    private static void addTotalRow(PdfPTable table, String... values) {
        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(values[i], bodyBoldFont()));
            cell.setBackgroundColor(ACCENT_LIGHT);
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(ACCENT);
            cell.setPadding(10);
            table.addCell(cell);
        }
    }

    // Adds first 3 cells of a 5-column row (for transactions table)
    private static void addPartialRow(PdfPTable table, BaseColor bg,
                                      String... values) {
        for (String v : values) table.addCell(styledCell(v, bodyFont(), bg));
    }

    private static PdfPCell styledCell(String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        return cell;
    }

    private static void addFooter(Document doc) throws Exception {
        Paragraph footer = new Paragraph(
                "\nGenerated by Piggy Pro  •  "
                        + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                smallFont());
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);
    }

    private static String ensureDir() {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists()) dir.mkdirs();
        return EXPORT_DIR;
    }

    private static String fmt(double v) {
        return String.format("%,.0f", v);
    }

    private static String formatMonth(String yyyyMM) {
        try {
            return LocalDate.parse(yyyyMM + "-01")
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        } catch (Exception e) { return yyyyMM; }
    }
}
