package com.playground.fondoahorro.application.importing.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;

import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_LOANS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PAYMENTS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PERSONS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_SAVINGS;

/** Builds a blank .xlsx with the four sheets, headers and one example row — the starting point for a bulk import. */
public final class ExcelTemplateGenerator {

    private ExcelTemplateGenerator() {
    }

    public static void write(OutputStream output) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);

            writeSheet(workbook, headerStyle, SHEET_PERSONS,
                    new String[]{"nombre", "fecha_nacimiento", "telefono"},
                    new String[]{"Juan Pérez", "15/03/1985", "3001234567"});

            writeSheet(workbook, headerStyle, SHEET_SAVINGS,
                    new String[]{"nombre_persona", "monto", "fecha", "medio_pago", "notas"},
                    new String[]{"Juan Pérez", "500000", "01/02/2026", "EFECTIVO", "Aporte mensual"});

            writeSheet(workbook, headerStyle, SHEET_LOANS,
                    new String[]{"id_prestamo", "nombre_persona", "capital", "tasa_interes_mensual", "fecha_prestamo",
                            "medio_pago", "notas"},
                    new String[]{"1", "Juan Pérez", "1000000", "3", "05/02/2026", "EFECTIVO", "Préstamo de ejemplo"});

            writeSheet(workbook, headerStyle, SHEET_PAYMENTS,
                    new String[]{"id_prestamo", "fecha_pago", "monto", "medio_pago", "notas"},
                    new String[]{"1", "05/03/2026", "30000", "EFECTIVO", "Pago de interés de marzo"});

            workbook.write(output);
        }
    }

    private static void writeSheet(XSSFWorkbook workbook, CellStyle headerStyle, String sheetName,
                                    String[] headers, String[] exampleRow) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < headers.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(col, 20 * 256);
        }
        Row exampleDataRow = sheet.createRow(1);
        for (int col = 0; col < exampleRow.length; col++) {
            exampleDataRow.createCell(col).setCellValue(exampleRow[col]);
        }
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }
}
