package com.playground.fondoahorro.application.importing.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the raw text of every cell in the import workbook's four sheets,
 * without interpreting or validating any of it — that's ImportValidator's
 * job. Every cell (whatever its native Excel type) comes out as a trimmed
 * string, in the same dd/MM/yyyy shape the rest of the app already uses for
 * dates, so downstream parsing can reuse MoneyFormatter/PercentageFormatter
 * exactly as the UI does.
 */
public final class ExcelWorkbookParser {

    public static final String SHEET_PERSONS = "Personas";
    public static final String SHEET_SAVINGS = "Ahorros";
    public static final String SHEET_LOANS = "Prestamos";
    public static final String SHEET_PAYMENTS = "Pagos";

    private static final DateTimeFormatter CELL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExcelWorkbookParser() {
    }

    public record RawPersonRow(int rowNumber, String name, String birthDate, String phone) {
    }

    public record RawSavingRow(int rowNumber, String personName, String amount, String date, String method, String notes) {
    }

    public record RawLoanRow(int rowNumber, String localLoanId, String personName, String principal, String interestRate,
                              String loanDate, String method, String notes) {
    }

    public record RawPaymentRow(int rowNumber, String localLoanId, String paymentDate, String amount, String method,
                                 String notes) {
    }

    public record RawWorkbook(List<RawPersonRow> persons, List<RawSavingRow> savings, List<RawLoanRow> loans,
                               List<RawPaymentRow> payments) {
    }

    public static RawWorkbook parse(InputStream input) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            List<RawPersonRow> persons = new ArrayList<>();
            for (List<String> row : readDataRows(workbook, SHEET_PERSONS, 2)) { // nombre, fecha_nacimiento, telefono
                persons.add(new RawPersonRow(rowNumber(row), cell(row, 1), cell(row, 2), cell(row, 3)));
            }
            List<RawSavingRow> savings = new ArrayList<>();
            for (List<String> row : readDataRows(workbook, SHEET_SAVINGS, 4)) { // nombre_persona, monto, fecha, medio_pago, notas
                savings.add(new RawSavingRow(rowNumber(row), cell(row, 1), cell(row, 2), cell(row, 3), cell(row, 4), cell(row, 5)));
            }
            List<RawLoanRow> loans = new ArrayList<>();
            // id_prestamo, nombre_persona, capital, tasa_interes_mensual, fecha_prestamo, medio_pago, notas
            for (List<String> row : readDataRows(workbook, SHEET_LOANS, 6)) {
                loans.add(new RawLoanRow(rowNumber(row), cell(row, 1), cell(row, 2), cell(row, 3), cell(row, 4),
                        cell(row, 5), cell(row, 6), cell(row, 7)));
            }
            List<RawPaymentRow> payments = new ArrayList<>();
            for (List<String> row : readDataRows(workbook, SHEET_PAYMENTS, 4)) { // id_prestamo, fecha_pago, monto, medio_pago, notas
                payments.add(new RawPaymentRow(rowNumber(row), cell(row, 1), cell(row, 2), cell(row, 3), cell(row, 4), cell(row, 5)));
            }
            return new RawWorkbook(persons, savings, loans, payments);
        }
    }

    private static int rowNumber(List<String> row) {
        return Integer.parseInt(row.get(0));
    }

    private static String cell(List<String> row, int index) {
        return index < row.size() ? row.get(index) : "";
    }

    /**
     * Reads every non-blank row after the header (row 0) from the given
     * sheet, as a list whose first element is always the 1-based Excel row
     * number (for error messages) followed by one string per data column.
     * Missing sheets yield no rows rather than an error — an import that
     * only has, say, Personas and Ahorros is still valid.
     */
    private static List<List<String>> readDataRows(Workbook workbook, String sheetName, int lastColumnIndex) {
        List<List<String>> rows = new ArrayList<>();
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            return rows;
        }
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row, lastColumnIndex)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            values.add(String.valueOf(rowIndex + 1)); // 1-based Excel row number, matches what the user sees
            for (int col = 0; col <= lastColumnIndex; col++) {
                values.add(cellToString(row.getCell(col)));
            }
            rows.add(values);
        }
        return rows;
    }

    private static boolean isBlankRow(Row row, int lastColumnIndex) {
        for (int col = 0; col <= lastColumnIndex; col++) {
            if (!cellToString(row.getCell(col)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().format(CELL_DATE_FORMAT)
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
