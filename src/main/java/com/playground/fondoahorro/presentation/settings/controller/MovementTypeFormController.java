package com.playground.fondoahorro.presentation.settings.controller;

import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.presentation.shared.Labels;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;

public class MovementTypeFormController {

    @FXML
    private Label kindLabel;
    @FXML
    private TextField nameField;
    @FXML
    private Label errorLabel;

    public void setContext(MovementKind kind, String existingName) {
        kindLabel.setText("Tipo de " + Labels.of(kind).toLowerCase());
        if (existingName != null) {
            nameField.setText(existingName);
        }
    }

    public Optional<String> validate() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isBlank()) {
            errorLabel.setText("Ingresa el nombre del tipo.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return Optional.empty();
        }
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        return Optional.of(name);
    }
}
