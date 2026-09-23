package com.playground.fondoahorro.presentation.loan.controller;

import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;
import com.playground.fondoahorro.presentation.loan.dto.LoanPaymentInput;

public final class LoanPaymentFormDialog {

    private LoanPaymentFormDialog() {
    }

    public static Optional<LoanPaymentInput> show(Loan loan) {
        try {
            FXMLLoader loader = new FXMLLoader(LoanPaymentFormDialog.class.getResource("/fxml/loan/loan_payment_form.fxml"));
            Parent content = loader.load();
            LoanPaymentFormController controller = loader.getController();
            controller.setLoan(loan);

            return Dialogs.showForm("Registrar pago", content, "Registrar pago", controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de pago.", e);
        }
    }
}
