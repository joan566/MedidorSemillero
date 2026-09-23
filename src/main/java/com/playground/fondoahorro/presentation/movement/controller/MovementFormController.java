package com.playground.fondoahorro.presentation.movement.controller;

import com.playground.fondoahorro.domain.inputport.MovementTypeService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.presentation.movement.dto.MovementInput;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;
import com.playground.fondoahorro.application.movement.service.MovementTypeServiceImpl;

public class MovementFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Fund> fundCombo;
    @FXML
    private ComboBox<MovementType> typeCombo;
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

    private final MovementTypeService movementTypeService = new MovementTypeServiceImpl(new JdbcMovementTypeRepository());
    private final PersonService personService = new PersonServiceImpl(new JdbcPersonRepository());

    private MovementKind kind;

    @FXML
    private void initialize() {
        MoneyTextFields.attachLiveFormatting(amountField);

        fundCombo.setItems(FXCollections.observableArrayList(Fund.values()));
        fundCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Fund fund) {
                return fund == null ? "" : Labels.of(fund);
            }

            @Override
            public Fund fromString(String string) {
                return null;
            }
        });
        fundCombo.getSelectionModel().selectFirst();

        typeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MovementType type) {
                return type == null ? "" : type.name();
            }

            @Override
            public MovementType fromString(String string) {
                return null;
            }
        });

        List<Person> persons = new ArrayList<>();
        persons.add(null);
        persons.addAll(personService.list(null, true));
        personCombo.setItems(FXCollections.observableArrayList(persons));
        personCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person person) {
                return person == null ? "Sin persona (fondo general)" : person.name();
            }

            @Override
            public Person fromString(String string) {
                return null;
            }
        });
        personCombo.getSelectionModel().selectFirst();

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

    public void setKind(MovementKind kind) {
        this.kind = kind;
        titleLabel.setText(kind == MovementKind.INCOME ? "Datos del ingreso" : "Datos del egreso");

        List<MovementType> types = movementTypeService.listByKind(kind, false).stream()
                .filter(type -> !type.isSystemType())
                .toList();
        typeCombo.setItems(FXCollections.observableArrayList(types));
        typeCombo.getSelectionModel().selectFirst();
        if (types.isEmpty()) {
            showError("No hay categorías personalizadas de " + Labels.of(kind).toLowerCase()
                    + ". Crea una en Configuración antes de registrar este movimiento.");
        }
    }

    public Optional<MovementInput> validate() {
        Fund fund = fundCombo.getValue();
        if (fund == null) {
            showError("Selecciona el fondo.");
            return Optional.empty();
        }
        MovementType type = typeCombo.getValue();
        if (type == null) {
            showError("Selecciona la categoría del movimiento.");
            return Optional.empty();
        }
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            showError("Ingresa un valor mayor a $0.");
            return Optional.empty();
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Selecciona la fecha del movimiento.");
            return Optional.empty();
        }
        if (date.isAfter(LocalDate.now())) {
            showError("La fecha del movimiento no puede ser futura.");
            return Optional.empty();
        }
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showError("Selecciona el medio de pago.");
            return Optional.empty();
        }
        hideError();
        Person person = personCombo.getValue();
        return Optional.of(new MovementInput(type.id(), fund, person == null ? null : person.id(),
                amount.get(), date, method, notesField.getText()));
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
