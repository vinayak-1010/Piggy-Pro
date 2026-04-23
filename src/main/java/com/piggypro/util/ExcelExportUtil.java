package com.piggypro.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ExcelExportUtil.java
 * ─────────────────────────────────────────────────────
 * Generates .xlsx expense reports using Apache POI.
 *
 * Usage:
 *   String path = ExcelExportUtil.exportCategoryReport(
 *       "Vinayak", catMap, from, to, totalExp, totalInc);
 *
 * Output saved to: ~/PiggyPro/exports/Report_YYYY-MM-DD.xlsx
 */
public class ExcelExportUtil {

    private static final String EXPORT_DIR = System.getProperty("user.home")
            + File.separator + "PiggyPro" + File.separator + "exports";

    // Accent blue: #2563EB
    private static final byte[] ACCENT_RGB  = {(byte)37,  (byte)99,  (byte)235};
    private static final byte[] HEADER_RGB  = {(byte)219, (byte)234, (byte)254};
    private static final byte[] ROW_ALT_RGB = {(byte)248, (byte)250, (byte)252};
    private static final byte[] TOTAL_RGB   = {(byte)239, (byte)246, (byte)255};

    // ══════════════════════════════════════════════
    // PUBLIC ENTRY POINTS
    // ══════════════════════════════════════════════

    public static String exportCategoryReport(
            String username,
            Map<String, Double> categoryMap,
            LocalDate from, LocalDate to,
            double totalExp, double totalInc) throws Exception {

        String filename = "Category_Report_" + from + "_to_" + to + ".xlsx";
        String path = ensureDir() + File.separator + filename;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Category Report");
            sheet.setDefaultColumnWidth(20);

            int rowIdx = 0;
            rowIdx = addTitle(wb, sheet, "Category Spending Report",
                    username, from, to, rowIdx);
            rowIdx = addSummaryRows(wb, sheet, totalExp, totalInc, rowIdx);
            rowIdx++; // blank

            // Header row
            String[] headers = {"Category", "Total Spent (Rs.)", "% of Total"};
            addHeaderRow(wb, sheet, headers, rowIdx++);

            double grand = categoryMap.values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            int dataRow = 0;
            for (Map.Entry<String, Double> e : categoryMap.entrySet()) {
                double pct = grand > 0 ? e.getValue() / grand * 100 : 0;
                Row row = sheet.createRow(rowIdx++);
                boolean alt = dataRow++ % 2 == 1;
                CellStyle cs = alt ? altStyle(wb) : normalStyle(wb);
                setCell(row, 0, e.getKey(), cs);
                setCell(row, 1, e.getValue(), cs);
                setCell(row, 2, String.format("%.1f%%", pct), cs);
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx);
            CellStyle ts = totalStyle(wb);
            setCell(totalRow, 0, "TOTAL", ts);
            setCell(totalRow, 1, grand, ts);
            setCell(totalRow, 2, "100.0%", ts);

            autoSize(sheet, 3);
            addInfoSheet(wb, username, from, to);
            wb.write(new FileOutputStream(path));
        }
        return path;
    }

    public static String exportMonthlyReport(
            String username,
            Map<String, Double> monthlyData,
            Map<String, Double> incomeData,
            LocalDate from, LocalDate to) throws Exception {

        String filename = "Monthly_Report_" + from + "_to_" + to + ".xlsx";
        String path = ensureDir() + File.separator + filename;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Monthly Report");
            sheet.setDefaultColumnWidth(20);

            int rowIdx = 0;
            rowIdx = addTitle(wb, sheet, "Monthly Spending Report",
                    username, from, to, rowIdx);
            rowIdx++;

            String[] headers = {"Month", "Expenses (Rs.)", "Income (Rs.)", "Net Savings (Rs.)"};
            addHeaderRow(wb, sheet, headers, rowIdx++);

            int dataRow = 0;
            for (Map.Entry<String, Double> e : monthlyData.entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                boolean alt = dataRow++ % 2 == 1;
                CellStyle cs = alt ? altStyle(wb) : normalStyle(wb);
                double inc = incomeData.getOrDefault(e.getKey(), 0.0);
                setCell(row, 0, formatMonth(e.getKey()), cs);
                setCell(row, 1, e.getValue(), cs);
                setCell(row, 2, inc, cs);
                setCell(row, 3, inc - e.getValue(), cs);
            }

            autoSize(sheet, 4);
            addInfoSheet(wb, username, from, to);
            wb.write(new FileOutputStream(path));
        }
        return path;
    }

    public static String exportAllTransactions(
            String username,
            List<Object[]> expenses,
            LocalDate from, LocalDate to,
            double totalExp, double totalInc) throws Exception {

        String filename = "Transactions_" + from + "_to_" + to + ".xlsx";
        String path = ensureDir() + File.separator + filename;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transactions");
            sheet.setDefaultColumnWidth(22);

            int rowIdx = 0;
            rowIdx = addTitle(wb, sheet, "All Transactions",
                    username, from, to, rowIdx);
            rowIdx = addSummaryRows(wb, sheet, totalExp, totalInc, rowIdx);
            rowIdx++;

            String[] headers = {"Date", "Description", "Category", "Amount (Rs.)", "Type"};
            addHeaderRow(wb, sheet, headers, rowIdx++);

            int dataRow = 0;
            for (Object[] e : expenses) {
                Row row = sheet.createRow(rowIdx++);
                boolean alt = dataRow++ % 2 == 1;
                CellStyle cs = alt ? altStyle(wb) : normalStyle(wb);
                setCell(row, 0, (String) e[0], cs);
                setCell(row, 1, (String) e[1], cs);
                setCell(row, 2, (String) e[2], cs);
                setCell(row, 3, (Double) e[3], cs);
                setCell(row, 4, (String) e[4], cs);
            }

            autoSize(sheet, 5);
            addInfoSheet(wb, username, from, to);
            wb.write(new FileOutputStream(path));
        }
        return path;
    }

    // ══════════════════════════════════════════════
    // STYLE HELPERS
    // ══════════════════════════════════════════════

    private static int addTitle(XSSFWorkbook wb, Sheet sheet,
                                String title, String username,
                                LocalDate from, LocalDate to, int rowIdx) {
        CellStyle ts = wb.createCellStyle();
        Font tf = wb.createFont();
        tf.setBold(true); tf.setFontHeightInPoints((short) 16);
        ts.setFont(tf);

        Row titleRow = sheet.createRow(rowIdx++);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue(title); tc.setCellStyle(ts);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        Row subRow = sheet.createRow(rowIdx++);
        CellStyle ss = wb.createCellStyle();
        Font sf = wb.createFont();
        sf.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        ss.setFont(sf);
        Cell sc = subRow.createCell(0);
        sc.setCellValue("Prepared for: " + username
                + "   |   Period: " + from.format(fmt) + " – " + to.format(fmt));
        sc.setCellStyle(ss);

        sheet.createRow(rowIdx++); // blank
        return rowIdx;
    }

    private static int addSummaryRows(XSSFWorkbook wb, Sheet sheet,
                                      double totalExp, double totalInc, int rowIdx) {
        CellStyle lbl = wb.createCellStyle();
        Font lf = wb.createFont(); lf.setBold(true);
        lbl.setFont(lf);

        Row r1 = sheet.createRow(rowIdx++);
        r1.createCell(0).setCellValue("Total Expenses:");
        Cell e1 = r1.createCell(1); e1.setCellValue(totalExp);
        r1.createCell(0).setCellStyle(lbl);

        Row r2 = sheet.createRow(rowIdx++);
        r2.createCell(0).setCellValue("Total Income:");
        r2.createCell(1).setCellValue(totalInc);

        Row r3 = sheet.createRow(rowIdx++);
        r3.createCell(0).setCellValue("Net Savings:");
        r3.createCell(1).setCellValue(totalInc - totalExp);

        return rowIdx;
    }

    private static void addHeaderRow(XSSFWorkbook wb, Sheet sheet,
                                     String[] headers, int rowIdx) {
        Row row = sheet.createRow(rowIdx);
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(new XSSFColor(ACCENT_RGB, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.LEFT);
        cs.setBorderBottom(BorderStyle.THIN);
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(f);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(cs);
        }
    }

    private static CellStyle normalStyle(XSSFWorkbook wb) {
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setBorderBottom(BorderStyle.HAIR);
        // Use XSSFCellStyle-specific method — BorderSide was removed in POI 5.x
        cs.setBottomBorderColor(
                new XSSFColor(new byte[]{(byte)228, (byte)232, (byte)242}, null));
        return cs;
    }

    private static CellStyle altStyle(XSSFWorkbook wb) {
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(new XSSFColor(ROW_ALT_RGB, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cs;
    }

    private static CellStyle totalStyle(XSSFWorkbook wb) {
        XSSFCellStyle cs = wb.createCellStyle();
        cs.setFillForegroundColor(new XSSFColor(TOTAL_RGB, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setBorderTop(BorderStyle.MEDIUM);
        Font f = wb.createFont(); f.setBold(true);
        cs.setFont(f);
        return cs;
    }

    private static void setCell(Row row, int col, String val, CellStyle cs) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(cs);
    }

    private static void setCell(Row row, int col, double val, CellStyle cs) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(cs);
    }

    private static void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private static void addInfoSheet(XSSFWorkbook wb, String username,
                                     LocalDate from, LocalDate to) {
        Sheet info = wb.createSheet("Report Info");
        info.createRow(0).createCell(0).setCellValue("Generated by: Piggy Pro");
        info.createRow(1).createCell(0).setCellValue("User: " + username);
        info.createRow(2).createCell(0).setCellValue("Generated at: "
                + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        info.createRow(3).createCell(0).setCellValue(
                "Period: " + from + " to " + to);
        info.autoSizeColumn(0);
    }

    private static String ensureDir() {
        File dir = new File(EXPORT_DIR);
        if (!dir.exists()) dir.mkdirs();
        return EXPORT_DIR;
    }

    private static String formatMonth(String yyyyMM) {
        try {
            return LocalDate.parse(yyyyMM + "-01")
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        } catch (Exception e) { return yyyyMM; }
    }
}