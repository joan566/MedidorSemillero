package com.playground.fondoahorro.application.importing.service;

import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawLoanRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawPaymentRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawPersonRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawSavingRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawWorkbook;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_LOANS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PAYMENTS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PERSONS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_SAVINGS;
import com.playground.fondoahorro.application.importing.dto.ImportValidationResult;
import com.playground.fondoahorro.application.importing.dto.ImportError;
import com.playground.fondoahorro.application.importing.dto.ImportPlan;

/**
 * Validates a parsed workbook end to end before anything is written to the
 * database: data types, dates, and every cross-sheet reference (a saving or
 * loan's nombre_persona, a payment's id_prestamo). Referential checks are
 * decoupled from field-level ones — a row with a bad fecha_nacimiento still
 * lets other rows reference that person by name, so one typo doesn't cascade
 * into a wall of unrelated errors.
 */
public final class ImportValidator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ImportValidator() {
    }

    public static ImportValidationResult validate(RawWorkbook raw) {
        List<ImportError> errors = new ArrayList<>();

        Set<String> personNames = new HashSet<>();
        Set<String> seenPersonNames = new HashSet<>();
        List<ImportPlan.PersonRow> personRows = new ArrayList<>();
        for (RawPersonRow row : raw.persons()) {
            String name = blankToNull(row.name());
            if (name != null) {
                personNames.add(name);
                if (!seenPersonNames.add(name)) {
                    errors.add(new ImportError(SHEET_PERSONS, row.rowNumber(), "El nombre \"" + name + "\" está repetido en esta hoja."));
                    continue;
                }
            }
            if (name == null) {
                errors.add(new ImportError(SHEET_PERSONS, row.rowNumber(), "Falta el nombre."));
                continue;
            }
            Optional<LocalDate> birthDate = parseDate(row.birthDate());
            if (birthDate.isEmpty()) {
                errors.add(new ImportError(SHEET_PERSONS, row.rowNumber(), "La fecha de nacimiento no es válida (usa dd/mm/aaaa)."));
                continue;
            }
            if (birthDate.get().isAfter(LocalDate.now())) {
                errors.add(new ImportError(SHEET_PERSONS, row.rowNumber(), "La fecha de nacimiento no puede ser futura."));
                continue;
            }
            personRows.add(new ImportPlan.PersonRow(row.rowNumber(), name, birthDate.get(), blankToNull(row.phone())));
        }

        Set<String> loanIds = new HashSet<>();
        Set<String> seenLoanIds = new HashSet<>();
        Map<String, LocalDate> loanDatesById = new HashMap<>();
        List<ImportPlan.LoanRow> loanRows = new ArrayList<>();
        for (RawLoanRow row : raw.loans()) {
            String localLoanId = blankToNull(row.localLoanId());
            if (localLoanId != null) {
                loanIds.add(localLoanId);
                if (!seenLoanIds.add(localLoanId)) {
                    errors.add(new ImportError(SHEET_LOANS, row.rowNumber(), "El id_prestamo \"" + localLoanId + "\" está repetido en esta hoja."));
                    continue;
                }
            }
            List<String> rowErrors = new ArrayList<>();
            if (localLoanId == null) {
                rowErrors.add("Falta el id_prestamo.");
            }
            String personName = blankToNull(row.personName());
            if (personName == null) {
                rowErrors.add("Falta el nombre de la persona.");
            } else if (!personNames.contains(personName)) {
                rowErrors.add("\"" + personName + "\" no aparece en la hoja Personas.");
            }
            Optional<Money> principal = parseMoney(row.principal());
            if (principal.isEmpty() || !principal.get().isPositive()) {
                rowErrors.add("El capital debe ser un valor mayor a $0.");
            }
            Optional<Integer> rateBps = parseInterestRateBps(row.interestRate());
            if (rateBps.isEmpty() || rateBps.get() < 0) {
                rowErrors.add("La tasa de interés mensual no es válida (por ejemplo, 3).");
            }
            Optional<LocalDate> loanDate = parseDate(row.loanDate());
            if (loanDate.isEmpty()) {
                rowErrors.add("La fecha del préstamo no es válida (usa dd/mm/aaaa).");
            } else if (loanDate.get().isAfter(LocalDate.now())) {
                rowErrors.add("La fecha del préstamo no puede ser futura.");
            }
            Optional<PaymentMethod> method = parsePaymentMethod(row.method());
            if (method.isEmpty()) {
                rowErrors.add("El medio de pago debe ser EFECTIVO o TRANSFERENCIA.");
            }
            if (!rowErrors.isEmpty()) {
                rowErrors.forEach(message -> errors.add(new ImportError(SHEET_LOANS, row.rowNumber(), message)));
                continue;
            }
            loanDatesById.put(localLoanId, loanDate.get());
            loanRows.add(new ImportPlan.LoanRow(row.rowNumber(), localLoanId, personName, principal.get(), rateBps.get(),
                    loanDate.get(), method.get(), blankToNull(row.notes())));
        }

        List<ImportPlan.SavingRow> savingRows = new ArrayList<>();
        for (RawSavingRow row : raw.savings()) {
            List<String> rowErrors = new ArrayList<>();
            String personName = blankToNull(row.personName());
            if (personName == null) {
                rowErrors.add("Falta el nombre de la persona.");
            } else if (!personNames.contains(personName)) {
                rowErrors.add("\"" + personName + "\" no aparece en la hoja Personas.");
            }
            Optional<Money> amount = parseMoney(row.amount());
            if (amount.isEmpty() || !amount.get().isPositive()) {
                rowErrors.add("El monto debe ser un valor mayor a $0.");
            }
            Optional<LocalDate> date = parseDate(row.date());
            if (date.isEmpty()) {
                rowErrors.add("La fecha no es válida (usa dd/mm/aaaa).");
            } else if (date.get().isAfter(LocalDate.now())) {
                rowErrors.add("La fecha no puede ser futura.");
            }
            Optional<PaymentMethod> method = parsePaymentMethod(row.method());
            if (method.isEmpty()) {
                rowErrors.add("El medio de pago debe ser EFECTIVO o TRANSFERENCIA.");
            }
            if (!rowErrors.isEmpty()) {
                rowErrors.forEach(message -> errors.add(new ImportError(SHEET_SAVINGS, row.rowNumber(), message)));
                continue;
            }
            savingRows.add(new ImportPlan.SavingRow(row.rowNumber(), personName, amount.get(), date.get(), method.get(),
                    blankToNull(row.notes())));
        }

        List<ImportPlan.PaymentRow> paymentRows = new ArrayList<>();
        for (RawPaymentRow row : raw.payments()) {
            List<String> rowErrors = new ArrayList<>();
            String localLoanId = blankToNull(row.localLoanId());
            if (localLoanId == null) {
                rowErrors.add("Falta el id_prestamo.");
            } else if (!loanIds.contains(localLoanId)) {
                rowErrors.add("El id_prestamo \"" + localLoanId + "\" no aparece en la hoja Prestamos.");
            }
            Optional<Money> amount = parseMoney(row.amount());
            if (amount.isEmpty() || !amount.get().isPositive()) {
                rowErrors.add("El monto debe ser un valor mayor a $0.");
            }
            Optional<LocalDate> paymentDate = parseDate(row.paymentDate());
            if (paymentDate.isEmpty()) {
                rowErrors.add("La fecha del pago no es válida (usa dd/mm/aaaa).");
            } else if (paymentDate.get().isAfter(LocalDate.now())) {
                rowErrors.add("La fecha del pago no puede ser futura.");
            } else if (localLoanId != null && loanDatesById.containsKey(localLoanId)
                    && paymentDate.get().isBefore(loanDatesById.get(localLoanId))) {
                rowErrors.add("La fecha del pago es anterior a la fecha del préstamo " + localLoanId + ".");
            }
            Optional<PaymentMethod> method = parsePaymentMethod(row.method());
            if (method.isEmpty()) {
                rowErrors.add("El medio de pago debe ser EFECTIVO o TRANSFERENCIA.");
            }
            if (!rowErrors.isEmpty()) {
                rowErrors.forEach(message -> errors.add(new ImportError(SHEET_PAYMENTS, row.rowNumber(), message)));
                continue;
            }
            paymentRows.add(new ImportPlan.PaymentRow(row.rowNumber(), localLoanId, paymentDate.get(), amount.get(),
                    method.get(), blankToNull(row.notes())));
        }

        if (!errors.isEmpty()) {
            return ImportValidationResult.failure(errors);
        }
        if (personRows.isEmpty() && savingRows.isEmpty() && loanRows.isEmpty() && paymentRows.isEmpty()) {
            return ImportValidationResult.failure(
                    List.of(new ImportError(SHEET_PERSONS, 0, "El archivo no tiene ninguna fila para importar.")));
        }
        return ImportValidationResult.success(new ImportPlan(personRows, savingRows, loanRows, paymentRows));
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private static Optional<Money> parseMoney(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String digitsOnly = text.replaceAll("[^0-9]", "");
        return digitsOnly.isBlank() ? Optional.empty() : Optional.of(Money.of(new BigDecimal(digitsOnly)));
    }

    private static Optional<Integer> parseInterestRateBps(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal percent = new BigDecimal(text.trim().replace(',', '.'));
            return Optional.of(percent.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseDate(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.trim(), DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<PaymentMethod> parsePaymentMethod(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return switch (text.trim().toUpperCase(Locale.ROOT)) {
            case "EFECTIVO", "CASH" -> Optional.of(PaymentMethod.CASH);
            case "TRANSFERENCIA", "TRANSFER" -> Optional.of(PaymentMethod.TRANSFER);
            default -> Optional.empty();
        };
    }
}
