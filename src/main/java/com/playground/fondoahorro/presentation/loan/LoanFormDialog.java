package com.playground.fondoahorro.presentation.loan;

import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;

public final class LoanFormDialog {

    private LoanFormDialog() {
    }

    public static Optional<LoanInput> show() {
        return show(null);
    }

    public static Optional<LoanInput> showForPerson(Person person) {
        return show(person);
    }

    private static Optional<LoanInput> show(Person fixedPerson) {
        try {
            FXMLLoader loader = new FXMLLoader(LoanFormDialog.class.getResource("/fxml/loan/loan_form.fxml"));
            Parent content = loader.load();
            LoanFormController controller = loader.getController();
            if (fixedPerson != null) {
                controller.setFixedPerson(fixedPerson);
            }

            return Dialogs.showForm("Nuevo préstamo", content, "Registrar préstamo", controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de préstamo.", e);
        }
    }
}
