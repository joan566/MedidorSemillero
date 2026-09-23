package com.playground.fondoahorro.presentation.loan.controller;

import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanInterestChargeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanPaymentRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcAppSettingsRepository;
import com.playground.fondoahorro.presentation.shared.Labels;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.MoneyTextFields;
import com.playground.fondoahorro.presentation.shared.PercentageFormatter;
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
import com.playground.fondoahorro.presentation.loan.dto.LoanInput;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;
import com.playground.fondoahorro.application.loan.service.LoanServiceImpl;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;

public class LoanFormController {

    @FXML
    private ComboBox<Person> personCombo;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField interestField;
    @FXML
    private ComboBox<PaymentMethod> methodCombo;
    @FXML
    private TextField notesField;
    @FXML
    private Label interestPreviewLabel;
    @FXML
    private Label errorLabel;

    private final PersonService personService = new PersonServiceImpl(new JdbcPersonRepository());
    private final LoanService loanService = new LoanServiceImpl(
            new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcLoanInterestChargeRepository(),
            new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
            new AppSettingsServiceImpl(new JdbcAppSettingsRepository()));

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
        interestField.setText(PercentageFormatter.format(loanService.defaultInterestRateBps()));

        amountField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        interestField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        updatePreview();
    }

    /**
     * Reflects Loan's own interest arithmetic in the UI before saving — no new
     * business rule, just a preview. Interest is no longer a fixed total: this
     * shows only the first month's charge, since every following month it's
     * recalculated on whatever principal balance is left by then.
     */
    private void updatePreview() {
        Optional<BigDecimal> amount = MoneyFormatter.parse(amountField.getText());
        Optional<Integer> interestBps = PercentageFormatter.parseToBasisPoints(interestField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0 || interestBps.isEmpty() || interestBps.get() < 0) {
            interestPreviewLabel.setText("—");
            return;
        }
        Loan preview = Loan.create(0, Money.of(amount.get()), interestBps.get(), LocalDate.now(), null);
        interestPreviewLabel.setText(MoneyFormatter.format(preview.monthlyInterestCharge()));
    }

    public void setFixedPerson(Person person) {
        personCombo.setValue(person);
        personCombo.setDisable(true);
    }

    public Optional<LoanInput> validate() {
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
            showError("Selecciona la fecha del préstamo.");
            return Optional.empty();
        }
        if (date.isAfter(LocalDate.now())) {
            showError("La fecha del préstamo no puede ser futura.");
            return Optional.empty();
        }
        Optional<Integer> interestBps = PercentageFormatter.parseToBasisPoints(interestField.getText());
        if (interestBps.isEmpty() || interestBps.get() < 0) {
            showError("Ingresa una tasa de interés válida (por ejemplo, 3).");
            return Optional.empty();
        }
        PaymentMethod method = methodCombo.getValue();
        if (method == null) {
            showError("Selecciona el medio de pago.");
            return Optional.empty();
        }
        hideError();
        return Optional.of(new LoanInput(person.id(), person.name(), amount.get(), interestBps.get(), date, method, notesField.getText()));
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
