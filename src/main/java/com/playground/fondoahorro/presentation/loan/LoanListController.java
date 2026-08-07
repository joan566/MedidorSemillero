package com.playground.fondoahorro.presentation.loan;

import com.playground.fondoahorro.application.loan.LoanService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.loan.Loan;
import com.playground.fondoahorro.domain.loan.LoanListItem;
import com.playground.fondoahorro.domain.loan.LoanStatus;
import com.playground.fondoahorro.domain.money.Money;
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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class LoanListController {

    private static final Logger log = LoggerFactory.getLogger(LoanListController.class);

    @FXML
    private ToggleButton activeToggle;
    @FXML
    private ToggleButton paidToggle;
    @FXML
    private ToggleButton allToggle;
    @FXML
    private TableView<LoanListItem> table;
    @FXML
    private TableColumn<LoanListItem, String> personColumn;
    @FXML
    private TableColumn<LoanListItem, String> dateColumn;
    @FXML
    private TableColumn<LoanListItem, String> amountColumn;
    @FXML
    private TableColumn<LoanListItem, String> totalColumn;
    @FXML
    private TableColumn<LoanListItem, String> paidColumn;
    @FXML
    private TableColumn<LoanListItem, String> outstandingColumn;
    @FXML
    private TableColumn<LoanListItem, String> statusColumn;
    @FXML
    private Label loanedMoneyLabel;
    @FXML
    private Label outstandingDebtLabel;
    @FXML
    private Label activeLoanCountLabel;

    private final LoanService loanService = new LoanService(
            new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcMovementRepository(),
            new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
            new AppSettingsService(new JdbcAppSettingsRepository()));

    private Consumer<Long> onLoanSelected;

    @FXML
    private void initialize() {
        ToggleGroup group = new ToggleGroup();
        activeToggle.setToggleGroup(group);
        paidToggle.setToggleGroup(group);
        allToggle.setToggleGroup(group);
        activeToggle.setSelected(true);
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            refresh();
        });

        personColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(data.getValue().personName()));
        dateColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(DateFormatter.format(data.getValue().loan().loanDate())));
        amountColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().loan().principalAmount())));
        totalColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().loan().totalAmount())));
        paidColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().loan().paidAmount())));
        outstandingColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().loan().outstandingAmount())));
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                data.getValue().loan().status() == LoanStatus.PAID ? "Pagado" : "Activo"));
        statusColumn.setCellFactory(Badges.cellFactory(status -> status.equals("Pagado") ? BadgeStyle.SUCCESS : BadgeStyle.WARNING));

        table.setPlaceholder(EmptyStates.of(Icon.LOANS, "Aún no hay préstamos registrados",
                "Cuando registres un préstamo aparecerá aquí.", "+ Nuevo préstamo", this::handleNewLoan));
        table.setRowFactory(tv -> {
            TableRow<LoanListItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && onLoanSelected != null) {
                    onLoanSelected.accept(row.getItem().loan().id());
                }
            });
            return row;
        });

        refresh();
    }

    public void setOnLoanSelected(Consumer<Long> handler) {
        this.onLoanSelected = handler;
    }

    @FXML
    private void handleNewLoan() {
        LoanFormDialog.show().ifPresent(input -> {
            String message = "¿Registrar préstamo de " + MoneyFormatter.format(Money.of(input.amount()))
                    + " para " + input.personName() + "?";
            if (!Dialogs.confirm("Confirmar préstamo", message, "Cancelar", "Registrar préstamo")) {
                return;
            }
            try {
                Loan created = loanService.createLoan(input.personId(), Money.of(input.amount()), input.interestRateBps(),
                        input.date(), input.method(), input.notes());
                refresh();
                if (onLoanSelected != null) {
                    onLoanSelected.accept(created.id());
                }
                Toast.show(table.getScene().getWindow(), "Préstamo registrado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar préstamo", e);
                Dialogs.error("Por ahora no fue posible registrar el préstamo. Intenta nuevamente.");
            }
        });
    }

    private void refresh() {
        LoanStatus filter = activeToggle.isSelected() ? LoanStatus.ACTIVE
                : paidToggle.isSelected() ? LoanStatus.PAID
                : null;
        table.setItems(FXCollections.observableArrayList(loanService.list(filter, null)));
        refreshSummary();
    }

    private void refreshSummary() {
        var activeLoans = loanService.list(LoanStatus.ACTIVE, null);
        Money loaned = activeLoans.stream().map(item -> item.loan().principalAmount()).reduce(Money.ZERO, Money::plus);
        Money outstanding = activeLoans.stream().map(item -> item.loan().outstandingAmount()).reduce(Money.ZERO, Money::plus);
        loanedMoneyLabel.setText(MoneyFormatter.format(loaned));
        outstandingDebtLabel.setText(MoneyFormatter.format(outstanding));
        activeLoanCountLabel.setText(String.valueOf(activeLoans.size()));
    }
}
