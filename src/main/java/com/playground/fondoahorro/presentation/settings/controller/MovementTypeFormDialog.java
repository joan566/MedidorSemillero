package com.playground.fondoahorro.presentation.settings.controller;

import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;

/** Modal create/rename form for a MovementType. Kind is fixed and shown as context, never edited. */
public final class MovementTypeFormDialog {

    private MovementTypeFormDialog() {
    }

    public static Optional<String> showForCreate(MovementKind kind) {
        return show("Nuevo tipo", kind, null, "Crear tipo");
    }

    public static Optional<String> showForRename(MovementType type) {
        return show("Renombrar tipo", type.kind(), type.name(), "Guardar cambios");
    }

    private static Optional<String> show(String title, MovementKind kind, String existingName, String actionText) {
        try {
            FXMLLoader loader = new FXMLLoader(MovementTypeFormDialog.class.getResource("/fxml/settings/movement_type_form.fxml"));
            Parent content = loader.load();
            MovementTypeFormController controller = loader.getController();
            controller.setContext(kind, existingName);

            return Dialogs.showForm(title, content, actionText, controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de tipo.", e);
        }
    }
}
