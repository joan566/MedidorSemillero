package com.playground.fondoahorro.presentation.loan.controller;

import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.presentation.shared.Labels;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.MoneyTextFields;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import com.playground.fondoahorro.presentation.loan.dto.LoanPaymentInput;

public class LoanPaymentFormController {

    @FXML
    private Label interestOwedLabel;
    @FXML
    private Label principalBalanceLabel;
    @FXML
    private Label breakdownPreviewLabel;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<PaymentMethod> methodCombo;
    @FXML
    private TextField notesField;
    @FXML
    private Label errorLabel;

    private Loan loan;

    @FXML
    private void initialize() {
        MoneyTextFields.attachLiveFormatting(amountField);

        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod method) {
                return method == null ? "" : Labels.of(method);
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null;
            }
        });
        methodCombo.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());

        amountField.textProperty().addListener((obs, oldVal, newVal) -> updateBreakdownPreview());
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
        interestOwedLabel.setText("Interés pendiente: " + MoneyFormatter.format(loan.interestOwed()));
        principalBalanceLabel.setText("Saldo de capital: " + MoneyFormatter.format(loan.principalBalance()));
        updateBreakdownPreview();
    }

    /**
     * Shows, before the payment is confirmed, exactly how it will be split —
     * interest first, then abono a capital — using the same rule
     * (Loan.previewPayment) that LoanService.registerPayment applies for real.
     */
    private void updateBreakdownPreview() {
        if (loan == null) {
            return;
        }
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            breakdownPreviewLabel.setText("—");
            return;
        }
        Money amountMoney = Money.of(amount.get());
        Loan.PaymentAllocation allocation = loan.previewPayment(amountMoney);
        if (amountMoney.isGreaterThan(loan.totalOwed())) {
            breakdownPreviewLabel.setText("Este monto supera la deuda pendiente.");
            return;
        }
        if (allocation.principalPortion().isPositive()) {
            breakdownPreviewLabel.setText("Cubre " + MoneyFormatter.format(allocation.interestPortion())
                    + " de interés y " + MoneyFormatter.format(allocation.principalPortion()) + " de abono a capital.");
        } else if (allocation.remainingInterestOwed().isPositive()) {
            breakdownPreviewLabel.setText("Cubre " + MoneyFormatter.format(allocation.interestPortion())
                    + " de interés. Quedan " + MoneyFormatter.format(allocation.remainingInterestOwed()) + " de interés pendiente.");
        } else {
            breakdownPreviewLabel.setText("Cubre " + MoneyFormatter.format(allocation.interestPortion()) + " de interés.");
        }
    }

    public Optional<LoanPaymentInput> validate() {
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            showError("Ingresa un valor mayor a $0.");
            return Optional.empty();
        }
        Money amountMoney = Money.of(amount.get());
        if (amountMoney.isGreaterThan(loan.totalOwed())) {
            showError("El pago no puede ser mayor a la deuda pendiente (" + MoneyFormatter.format(loan.totalOwed()) + ").");
            return Optional.empty();
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Selecciona la fecha del pago.");
            return Optional.empty();
        }
        if (date.isAfter(LocalDate.now())) {
            showError("La fecha del pago no puede ser futura.");
            return Optional.empty();
        }
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showError("Selecciona el medio de pago.");
            return Optional.empty();
        }
        hideError();
        return Optional.of(new LoanPaymentInput(amount.get(), date, method, notesField.getText()));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
