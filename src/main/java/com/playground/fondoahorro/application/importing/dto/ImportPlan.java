package com.playground.fondoahorro.application.importing.dto;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.time.LocalDate;
import java.util.List;

/**
 * A fully validated, typed set of rows ready to write to the database — the
 * output of ImportValidator when a workbook has no errors, and the input to
 * ImportService. localLoanId here is the id_prestamo the user made up in the
 * spreadsheet purely to link a Pagos row to its Prestamos row; it has no
 * relationship to the real database id a loan gets once created.
 */
public record ImportPlan(List<PersonRow> persons, List<SavingRow> savings, List<LoanRow> loans, List<PaymentRow> payments) {

    public record PersonRow(int rowNumber, String name, LocalDate birthDate, String phone) {
    }

    public record SavingRow(int rowNumber, String personName, Money amount, LocalDate date, PaymentMethod method, String notes) {
    }

    public record LoanRow(int rowNumber, String localLoanId, String personName, Money principal, int interestRateBps,
                           LocalDate loanDate, PaymentMethod method, String notes) {
    }

    public record PaymentRow(int rowNumber, String localLoanId, LocalDate paymentDate, Money amount, PaymentMethod method,
                              String notes) {
    }
}
