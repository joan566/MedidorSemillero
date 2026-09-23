package com.playground.fondoahorro.application.importing.service;

import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawLoanRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawPaymentRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawPersonRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawSavingRow;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.RawWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.playground.fondoahorro.application.importing.dto.ImportValidationResult;
import com.playground.fondoahorro.application.importing.dto.ImportPlan;

class ImportValidatorTest {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String date(LocalDate value) {
        return value.format(DATE);
    }

    private static RawWorkbook workbookWith(List<RawPersonRow> persons, List<RawSavingRow> savings,
                                             List<RawLoanRow> loans, List<RawPaymentRow> payments) {
        return new RawWorkbook(persons, savings, loans, payments);
    }

    private static RawPersonRow validPerson(int row, String name) {
        return new RawPersonRow(row, name, "15/03/1985", "3001234567");
    }

    private static RawLoanRow validLoan(int row, String localLoanId, String personName) {
        return new RawLoanRow(row, localLoanId, personName, "100000", "3", date(LocalDate.now().minusMonths(2)), "EFECTIVO", null);
    }

    @Test
    void aFullyValidWorkbookProducesNoErrorsAndAMatchingPlan() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(new RawSavingRow(2, "Juan Pérez", "500000", date(LocalDate.now()), "EFECTIVO", "aporte")),
                List.of(validLoan(2, "1", "Juan Pérez")),
                List.of(new RawPaymentRow(2, "1", date(LocalDate.now().minusMonths(1)), "3000", "EFECTIVO", "interes")));

        ImportValidationResult result = ImportValidator.validate(raw);

        assertTrue(result.isValid());
        ImportPlan plan = result.plan().orElseThrow();
        assertEquals(1, plan.persons().size());
        assertEquals(1, plan.savings().size());
        assertEquals(1, plan.loans().size());
        assertEquals(1, plan.payments().size());
    }

    @Test
    void anEmptyWorkbookIsRejected() {
        RawWorkbook raw = workbookWith(List.of(), List.of(), List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
    }

    @Test
    void aSavingReferencingAnUnknownPersonIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(new RawSavingRow(2, "Maria Desconocida", "500000", date(LocalDate.now()), "EFECTIVO", null)),
                List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("no aparece en la hoja Personas"));
    }

    @Test
    void aPaymentReferencingAnUnknownLoanIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(),
                List.of(validLoan(2, "1", "Juan Pérez")),
                List.of(new RawPaymentRow(2, "99", date(LocalDate.now()), "3000", "EFECTIVO", null)));

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("no aparece en la hoja Prestamos"));
    }

    @Test
    void aFutureDateIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(new RawSavingRow(2, "Juan Pérez", "500000", date(LocalDate.now().plusDays(1)), "EFECTIVO", null)),
                List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().toLowerCase().contains("futura"));
    }

    @Test
    void aDuplicatePersonNameWithinTheSheetIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez"), validPerson(3, "Juan Pérez")),
                List.of(), List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("repetido"));
    }

    @Test
    void aDuplicateLocalLoanIdWithinTheSheetIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(),
                List.of(validLoan(2, "1", "Juan Pérez"), validLoan(3, "1", "Juan Pérez")),
                List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("repetido"));
    }

    @Test
    void aPaymentDatedBeforeItsOwnLoanIsRejected() {
        LocalDate loanDate = LocalDate.now().minusMonths(1);
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(),
                List.of(new RawLoanRow(2, "1", "Juan Pérez", "100000", "3", date(loanDate), "EFECTIVO", null)),
                List.of(new RawPaymentRow(2, "1", date(loanDate.minusDays(5)), "3000", "EFECTIVO", null)));

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("anterior a la fecha del préstamo"));
    }

    @Test
    void anInvalidPaymentMethodTextIsRejected() {
        RawWorkbook raw = workbookWith(
                List.of(validPerson(2, "Juan Pérez")),
                List.of(new RawSavingRow(2, "Juan Pérez", "500000", date(LocalDate.now()), "BITCOIN", null)),
                List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("EFECTIVO o TRANSFERENCIA"));
    }

    /** Regression guard: the template's example row must itself pass validation — catches column mismatches between the generator and the parser. */
    @Test
    void theGeneratedTemplateItselfParsesAndValidatesSuccessfully() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelTemplateGenerator.write(out);
        RawWorkbook raw = ExcelWorkbookParser.parse(new ByteArrayInputStream(out.toByteArray()));

        ImportValidationResult result = ImportValidator.validate(raw);

        assertTrue(result.isValid(), () -> "template example row failed validation: " + result.errors());
    }

    @Test
    void aMissingPersonNameIsRejectedWithoutCrashing() {
        RawWorkbook raw = workbookWith(
                List.of(new RawPersonRow(2, "", "15/03/1985", null)),
                List.of(), List.of(), List.of());

        ImportValidationResult result = ImportValidator.validate(raw);

        assertFalse(result.isValid());
        assertTrue(result.errors().get(0).message().contains("nombre"));
    }
}
