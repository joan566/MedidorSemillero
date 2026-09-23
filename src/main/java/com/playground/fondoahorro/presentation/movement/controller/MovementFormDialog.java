package com.playground.fondoahorro.presentation.movement.controller;

import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;
import com.playground.fondoahorro.presentation.movement.dto.MovementInput;

/** Modal "Registrar ingreso/egreso" form for custom (non-system) movement types. */
public final class MovementFormDialog {

    private MovementFormDialog() {
    }

    public static Optional<MovementInput> showForCreate(MovementKind kind) {
        try {
            FXMLLoader loader = new FXMLLoader(MovementFormDialog.class.getResource("/fxml/movement/movement_form.fxml"));
            Parent content = loader.load();
            MovementFormController controller = loader.getController();
            controller.setKind(kind);

            String title = kind == MovementKind.INCOME ? "Registrar ingreso" : "Registrar egreso";
            return Dialogs.showForm(title, content, title, controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de movimiento.", e);
        }
    }
}
