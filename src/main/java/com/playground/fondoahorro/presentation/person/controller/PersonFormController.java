package com.playground.fondoahorro.presentation.person.controller;

import com.playground.fondoahorro.domain.entity.Person;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.Optional;
import com.playground.fondoahorro.presentation.person.dto.PersonInput;

public class PersonFormController {

    @FXML
    private TextField nameField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private TextField phoneField;
    @FXML
    private Label errorLabel;

    public void setPerson(Person person) {
        nameField.setText(person.name());
        birthDatePicker.setValue(person.birthDate());
        phoneField.setText(person.phone());
    }

    public Optional<PersonInput> validate() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        LocalDate birthDate = birthDatePicker.getValue();
        String phone = phoneField.getText();

        if (name.isBlank()) {
            showError("Ingresa el nombre de la persona.");
            return Optional.empty();
        }
        if (birthDate == null) {
            showError("Selecciona la fecha de nacimiento.");
            return Optional.empty();
        }
        if (birthDate.isAfter(LocalDate.now())) {
            showError("La fecha de nacimiento no puede ser futura.");
            return Optional.empty();
        }
        hideError();
        return Optional.of(new PersonInput(name, birthDate, phone));
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
