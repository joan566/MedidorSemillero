package com.playground.fondoahorro.presentation.loan;

import com.playground.fondoahorro.application.loan.LoanService;
import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.loan.Loan;
import com.playground.fondoahorro.domain.loan.LoanPayment;
import com.playground.fondoahorro.domain.loan.LoanStatus;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.infrastructure.loan.JdbcLoanPaymentRepository;
import com.playground.fondoahorro.infrastructure.loan.JdbcLoanRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.settings.JdbcAppSettingsRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.Labels;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.PercentageFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LoanDetailController {

    private static final Logger log = LoggerFactory.getLogger(LoanDetailController.class);

    @FXML
    private Label personLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label principalLabel;
    @FXML
    private Label interestLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label paidLabel;
    @FXML
    private Label outstandingLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label notesLabel;
    @FXML
    private Button registerPaymentButton;
    @FXML
    private TableView<LoanPayment> paymentsTable;
    @FXML
    private TableColumn<LoanPayment, String> paymentDateColumn;
    @FXML
    private TableColumn<LoanPayment, String> paymentAmountColumn;
    @FXML
    private TableColumn<LoanPayment, String> paymentMethodColumn;
    @FXML
    private TableColumn<LoanPayment, String> paymentNotesColumn;

    private final LoanService loanService = new LoanService(
            new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcMovementRepository(),
            new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
            new AppSettingsService(new JdbcAppSettingsRepository()));
    private final PersonService personService = new PersonService(new JdbcPersonRepository());

    private long loanId;
    private Runnable onBack;

    @FXML
    private void initialize() {
        paymentDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().paymentDate())));
        paymentAmountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().amount())));
        paymentMethodColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Labels.of(data.getValue().paymentMethod())));
        paymentNotesColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Objects.requireNonNullElse(data.getValue().notes(), "—")));
        paymentsTable.setPlaceholder(EmptyStates.of(Icon.LOANS, "Aún no hay pagos registrados",
                "Los pagos que registres para este préstamo aparecerán aquí."));
    }

    public void setLoanId(long loanId) {
        this.loanId = loanId;
        load();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    private void load() {
        Loan loan = loanService.findById(loanId).orElseThrow(() -> new IllegalStateException("El préstamo no existe."));
        Person person = personService.findById(loan.personId()).orElseThrow();

        personLabel.setText(person.name());
        Label statusBadge = loan.status() == LoanStatus.PAID
                ? Badges.of("Pagado", BadgeStyle.SUCCESS)
                : Badges.of("Activo", BadgeStyle.WARNING);
        statusLabel.setText(statusBadge.getText());
        statusLabel.getStyleClass().setAll(statusBadge.getStyleClass());
        principalLabel.setText(MoneyFormatter.format(loan.principalAmount()));
        interestLabel.setText(MoneyFormatter.format(loan.interestAmount()) + " (" + PercentageFormatter.format(loan.interestRateBps()) + "%)");
        totalLabel.setText(MoneyFormatter.format(loan.totalAmount()));
        paidLabel.setText(MoneyFormatter.format(loan.paidAmount()));
        outstandingLabel.setText(MoneyFormatter.format(loan.outstandingAmount()));
        dateLabel.setText(DateFormatter.format(loan.loanDate()));
        notesLabel.setText(Objects.requireNonNullElse(loan.notes(), "—"));

        registerPaymentButton.setDisable(loan.status() == LoanStatus.PAID);
        registerPaymentButton.setVisible(loan.status() != LoanStatus.PAID);

        paymentsTable.setItems(FXCollections.observableArrayList(loanService.paymentsForLoan(loanId)));
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void handleRegisterPayment() {
        Loan loan = loanService.findById(loanId).orElseThrow();
        LoanPaymentFormDialog.show(loan).ifPresent(input -> {
            String message = "¿Registrar pago de " + MoneyFormatter.format(Money.of(input.amount()))
                    + " para el préstamo de " + personService.findById(loan.personId()).map(Person::name).orElse("") + "?";
            if (!Dialogs.confirm("Confirmar pago", message, "Cancelar", "Registrar pago")) {
                return;
            }
            registerPaymentButton.setDisable(true);
            try {
                loanService.registerPayment(loanId, Money.of(input.amount()), input.date(), input.method(), input.notes());
                load(); // recomputes registerPaymentButton's disabled state from the loan's new status
                Toast.show(registerPaymentButton.getScene().getWindow(), "Pago registrado.");
            } catch (IllegalArgumentException e) {
                registerPaymentButton.setDisable(false);
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                registerPaymentButton.setDisable(false);
                log.error("Error al registrar pago del préstamo {}", loanId, e);
                Dialogs.error("Por ahora no fue posible registrar el pago. Intenta nuevamente.");
            }
        });
    }
}
