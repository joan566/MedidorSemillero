package com.playground.fondoahorro.presentation.savings;

import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.savings.Saving;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
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

public class SavingFormController {

    @FXML
    private ComboBox<Person> personCombo;
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

    private final PersonService personService = new PersonService(new JdbcPersonRepository());

    @FXML
    private void initialize() {
        MoneyTextFields.attachLiveFormatting(amountField);

        personCombo.setItems(FXCollections.observableArrayList(personService.list(null, true)));
        personCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person person) {
                return person == null ? "" : person.name();
            }

            @Override
            public Person fromString(String string) {
                return null;
            }
        });

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

    public void setFixedPerson(Person person) {
        personCombo.setValue(person);
        personCombo.setDisable(true);
    }

    public void setSaving(Saving saving) {
        Person owner = personCombo.getItems().stream()
                .filter(p -> p.id() == saving.personId())
                .findFirst()
                .orElse(null);
        personCombo.setValue(owner);
        personCombo.setDisable(true);
        amountField.setText(MoneyFormatter.format(saving.amount()));
        datePicker.setValue(saving.date());
        methodCombo.setValue(saving.paymentMethod());
        notesField.setText(saving.notes());
    }

    public Optional<SavingInput> validate() {
        Person person = personCombo.getValue();
        if (person == null) {
            showError("Selecciona la persona.");
            return Optional.empty();
        }
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            showError("Ingresa un valor mayor a $0.");
            return Optional.empty();
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Selecciona la fecha del ahorro.");
            return Optional.empty();
        }
        if (date.isAfter(LocalDate.now())) {
            showError("La fecha del ahorro no puede ser futura.");
            return Optional.empty();
        }
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showError("Selecciona el medio de pago.");
            return Optional.empty();
        }
        hideError();
        return Optional.of(new SavingInput(person.id(), amount.get(), date, method, notesField.getText()));
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
