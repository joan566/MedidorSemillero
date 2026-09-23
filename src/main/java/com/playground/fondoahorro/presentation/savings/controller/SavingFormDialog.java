package com.playground.fondoahorro.presentation.savings.controller;

import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.entity.Saving;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;
import com.playground.fondoahorro.presentation.savings.dto.SavingInput;

/** Modal "Registrar ahorro" form. No confirmation step afterward — registering a saving is not a sensitive operation. */
public final class SavingFormDialog {

    private SavingFormDialog() {
    }

    public static Optional<SavingInput> show() {
        return show(null, null);
    }

    public static Optional<SavingInput> showForPerson(Person person) {
        return show(person, null);
    }

    public static Optional<SavingInput> showForEdit(Saving saving) {
        return show(null, saving);
    }

    private static Optional<SavingInput> show(Person fixedPerson, Saving existingSaving) {
        try {
            FXMLLoader loader = new FXMLLoader(SavingFormDialog.class.getResource("/fxml/savings/saving_form.fxml"));
            Parent content = loader.load();
            SavingFormController controller = loader.getController();
            if (fixedPerson != null) {
                controller.setFixedPerson(fixedPerson);
            }
            if (existingSaving != null) {
                controller.setSaving(existingSaving);
            }

            String title = existingSaving != null ? "Editar ahorro" : "Registrar ahorro";
            String actionText = existingSaving != null ? "Guardar cambios" : "Registrar ahorro";
            return Dialogs.showForm(title, content, actionText, controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de ahorro.", e);
        }
    }
}
