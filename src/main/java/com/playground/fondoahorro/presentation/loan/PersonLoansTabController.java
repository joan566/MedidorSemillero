package com.playground.fondoahorro.presentation.loan;

import com.playground.fondoahorro.application.loan.LoanService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.loan.Loan;
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
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/** Embedded in the person detail screen's "Préstamos" tab, scoped to a single person. */
public class PersonLoansTabController {

    private static final Logger log = LoggerFactory.getLogger(PersonLoansTabController.class);

    @FXML
    private TableView<Loan> table;
    @FXML
    private TableColumn<Loan, String> dateColumn;
    @FXML
    private TableColumn<Loan, String> amountColumn;
    @FXML
    private TableColumn<Loan, String> totalColumn;
    @FXML
    private TableColumn<Loan, String> outstandingColumn;
    @FXML
    private TableColumn<Loan, String> statusColumn;

    private final LoanService loanService = new LoanService(
            new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcMovementRepository(),
            new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
            new AppSettingsService(new JdbcAppSettingsRepository()));

    private Person person;
    private Runnable onChanged;
    private Consumer<Long> onLoanSelected;

    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().loanDate())));
        amountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().principalAmount())));
        totalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().totalAmount())));
        outstandingColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().outstandingAmount())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status() == LoanStatus.PAID ? "Pagado" : "Activo"));
        statusColumn.setCellFactory(Badges.cellFactory(status -> status.equals("Pagado") ? BadgeStyle.SUCCESS : BadgeStyle.WARNING));

        table.setPlaceholder(EmptyStates.of(Icon.LOANS, "Aún no hay préstamos",
                "Cuando registres un préstamo para esta persona aparecerá aquí.", "+ Nuevo préstamo", this::handleNewLoan));
        table.setRowFactory(tv -> {
            TableRow<Loan> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && onLoanSelected != null) {
                    onLoanSelected.accept(row.getItem().id());
                }
            });
            return row;
        });
    }

    public void setPerson(Person person) {
        this.person = person;
        refresh();
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public void setOnLoanSelected(Consumer<Long> onLoanSelected) {
        this.onLoanSelected = onLoanSelected;
    }

    @FXML
    private void handleNewLoan() {
        LoanFormDialog.showForPerson(person).ifPresent(input -> {
            String message = "¿Registrar préstamo de " + MoneyFormatter.format(Money.of(input.amount()))
                    + " para " + input.personName() + "?";
            if (!Dialogs.confirm("Confirmar préstamo", message, "Cancelar", "Registrar préstamo")) {
                return;
            }
            try {
                loanService.createLoan(input.personId(), Money.of(input.amount()), input.interestRateBps(),
                        input.date(), input.method(), input.notes());
                refresh();
                if (onChanged != null) {
                    onChanged.run();
                }
                Toast.show(table.getScene().getWindow(), "Préstamo registrado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar préstamo para la persona {}", person.id(), e);
                Dialogs.error("Por ahora no fue posible registrar el préstamo. Intenta nuevamente.");
            }
        });
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(loanService.historyForPerson(person.id())));
    }
}
