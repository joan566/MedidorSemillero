package com.playground.fondoahorro.presentation.shared;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.Optional;
import java.util.function.Supplier;

/** Confirmation/error/info alerts and modal forms shared by every screen, in plain human language (no stack traces). */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * A modal form dialog with explicit "Cancelar" / actionText buttons (e.g. "Registrar ahorro") instead of
     * generic OK/Cancel. {@code validate} is invoked both to gate the action button and to build the result,
     * mirroring each *FormController's existing validate() contract.
     */
    public static <T> Optional<T> showForm(String title, Parent content, String actionText, Supplier<Optional<T>> validate) {
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType action = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);

        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, action);
        style(dialog.getDialogPane());

        Button actionButton = (Button) dialog.getDialogPane().lookupButton(action);
        actionButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (validate.get().isEmpty()) {
                event.consume();
            }
        });
        dialog.setResultConverter(buttonType -> buttonType == action ? validate.get().orElse(null) : null);

        return dialog.showAndWait();
    }

    /**
     * cancelText/actionText are explicit (e.g. "Cancelar" / "Registrar pago") rather than
     * relying on ButtonType.YES/NO, whose label depends on the JVM's default locale.
     */
    public static boolean confirm(String title, String message, String cancelText, String actionText) {
        ButtonType cancel = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType action = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, cancel, action);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        style(alert);
        return alert.showAndWait().filter(button -> button == action).isPresent();
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("No fue posible completar la acción");
        alert.setHeaderText(null);
        alert.setGraphic(null);
        style(alert);
        alert.showAndWait();
    }

    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        style(alert);
        alert.showAndWait();
    }

    private static void style(Alert alert) {
        style(alert.getDialogPane());
    }

    private static void style(DialogPane pane) {
        pane.getStylesheets().add(Dialogs.class.getResource("/css/app.css").toExternalForm());
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(buttonType);
            boolean isPrimaryAction = buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE;
            button.getStyleClass().add(isPrimaryAction ? "primary-button" : "secondary-button");
        }
    }
}
