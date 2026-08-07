package com.playground.fondoahorro.presentation.settlement;

import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;

public final class YearPickerDialog {

    private YearPickerDialog() {
    }

    public static Optional<Integer> show(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(YearPickerDialog.class.getResource("/fxml/settlement/year_picker.fxml"));
            Parent content = loader.load();
            YearPickerController controller = loader.getController();

            return Dialogs.showForm(title, content, "Continuar", () -> Optional.ofNullable(controller.getSelectedYear()));
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el selector de año.", e);
        }
    }
}
