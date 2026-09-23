package com.playground.fondoahorro.presentation.birthday.controller;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.entity.Person;
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
import com.playground.fondoahorro.presentation.birthday.dto.BirthdayGiftInput;

public class BirthdayGiftFormController {

    @FXML
    private Label personLabel;
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
    }

    public void setContext(Person person, Money defaultAmount) {
        personLabel.setText(person.name());
        amountField.setText(defaultAmount.toPesos().toBigInteger().toString());
    }

    public Optional<BirthdayGiftInput> validate() {
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            showError("Ingresa un valor mayor a $0.");
            return Optional.empty();
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Selecciona la fecha del regalo.");
            return Optional.empty();
        }
        if (date.isAfter(LocalDate.now())) {
            showError("La fecha del regalo no puede ser futura.");
            return Optional.empty();
        }
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showError("Selecciona el medio de pago.");
            return Optional.empty();
        }
        hideError();
        return Optional.of(new BirthdayGiftInput(amount.get(), date, method, notesField.getText()));
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
