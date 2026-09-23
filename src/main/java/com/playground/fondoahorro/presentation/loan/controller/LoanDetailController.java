package com.playground.fondoahorro.presentation.loan.controller;

import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.enums.LoanInterestChargeStatus;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanInterestChargeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanPaymentRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcAppSettingsRepository;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.playground.fondoahorro.presentation.loan.dto.LoanTimelineEntry;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;
import com.playground.fondoahorro.application.loan.service.LoanServiceImpl;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;

public class LoanDetailController {

    private static final Logger log = LoggerFactory.getLogger(LoanDetailController.class);

    @FXML
    private Label personLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label principalLabel;
    @FXML
    private Label rateLabel;
    @FXML
    private Label principalBalanceLabel;
    @FXML
    private Label interestOwedLabel;
    @FXML
    private Label paidLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label notesLabel;
    @FXML
    private Button registerPaymentButton;
    @FXML
    private ToggleButton allToggle;
    @FXML
    private ToggleButton paymentsToggle;
    @FXML
    private ToggleButton chargesToggle;
    @FXML
    private ToggleButton pendingToggle;
    @FXML
    private TableView<LoanTimelineEntry> timelineTable;
    @FXML
    private TableColumn<LoanTimelineEntry, String> timelineDateColumn;
    @FXML
    private TableColumn<LoanTimelineEntry, String> timelineTypeColumn;
    @FXML
    private TableColumn<LoanTimelineEntry, String> timelineDetailColumn;
    @FXML
    private TableColumn<LoanTimelineEntry, String> timelineAmountColumn;
    @FXML
    private TableColumn<LoanTimelineEntry, String> timelineStatusColumn;

    private final LoanService loanService = new LoanServiceImpl(
            new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcLoanInterestChargeRepository(),
            new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
            new AppSettingsServiceImpl(new JdbcAppSettingsRepository()));
    private final PersonService personService = new PersonServiceImpl(new JdbcPersonRepository());

    private long loanId;
    private Runnable onBack;
    private List<LoanTimelineEntry> allEntries = List.of();

    @FXML
    private void initialize() {
        ToggleGroup group = new ToggleGroup();
        allToggle.setToggleGroup(group);
        paymentsToggle.setToggleGroup(group);
        chargesToggle.setToggleGroup(group);
        pendingToggle.setToggleGroup(group);
        allToggle.setSelected(true);
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            refreshTimeline();
        });

        timelineDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().date())));

        timelineTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(switch (data.getValue()) {
            case LoanTimelineEntry.PaymentEntry p -> "Pago";
            case LoanTimelineEntry.ChargeEntry c -> "Cobro";
        }));
        timelineTypeColumn.setCellFactory(Badges.cellFactory(type -> "Pago".equals(type) ? BadgeStyle.SUCCESS : BadgeStyle.INFO));

        timelineDetailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(switch (data.getValue()) {
            case LoanTimelineEntry.PaymentEntry p -> "Interés " + MoneyFormatter.format(p.payment().interestPortion())
                    + " · Abono a capital " + MoneyFormatter.format(p.payment().principalPortion())
                    + " · " + Labels.of(p.payment().paymentMethod());
            case LoanTimelineEntry.ChargeEntry c -> "Saldo de capital " + MoneyFormatter.format(c.charge().principalBalance())
                    + " · Pagado " + MoneyFormatter.format(c.charge().paidAmount());
        }));

        timelineAmountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(switch (data.getValue()) {
            case LoanTimelineEntry.PaymentEntry p -> MoneyFormatter.format(p.payment().amount());
            case LoanTimelineEntry.ChargeEntry c -> MoneyFormatter.format(c.charge().interestAmount());
        }));

        timelineStatusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(switch (data.getValue()) {
            case LoanTimelineEntry.PaymentEntry p -> "—";
            case LoanTimelineEntry.ChargeEntry c -> chargeStatusLabel(c.charge().status());
        }));
        timelineStatusColumn.setCellFactory(Badges.cellFactory(status -> switch (status) {
            case "Pagado" -> BadgeStyle.SUCCESS;
            case "Parcial" -> BadgeStyle.WARNING;
            case "Pendiente" -> BadgeStyle.DANGER;
            default -> BadgeStyle.NEUTRAL;
        }));

        timelineTable.setPlaceholder(EmptyStates.of(Icon.LOANS, "Aún no hay movimientos",
                "Los pagos y cobros de interés de este préstamo aparecerán aquí."));
    }

    private static String chargeStatusLabel(LoanInterestChargeStatus status) {
        return switch (status) {
            case PAID -> "Pagado";
            case PARTIAL -> "Parcial";
            case PENDING -> "Pendiente";
        };
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
        rateLabel.setText(PercentageFormatter.format(loan.interestRateBps()) + "% mensual");
        principalBalanceLabel.setText(MoneyFormatter.format(loan.principalBalance()));
        interestOwedLabel.setText(MoneyFormatter.format(loan.interestOwed()));
        paidLabel.setText(MoneyFormatter.format(loan.paidAmount()));
        dateLabel.setText(DateFormatter.format(loan.loanDate()));
        notesLabel.setText(Objects.requireNonNullElse(loan.notes(), "—"));

        registerPaymentButton.setDisable(loan.status() == LoanStatus.PAID);
        registerPaymentButton.setVisible(loan.status() != LoanStatus.PAID);

        List<LoanTimelineEntry> entries = new ArrayList<>();
        loanService.paymentsForLoan(loanId).forEach(payment -> entries.add(new LoanTimelineEntry.PaymentEntry(payment)));
        loanService.chargesForLoan(loanId).forEach(charge -> entries.add(new LoanTimelineEntry.ChargeEntry(charge)));
        entries.sort(Comparator.comparing(LoanTimelineEntry::date).reversed());
        allEntries = entries;
        refreshTimeline();
    }

    private void refreshTimeline() {
        List<LoanTimelineEntry> filtered = allEntries.stream()
                .filter(entry -> {
                    if (paymentsToggle.isSelected()) {
                        return entry instanceof LoanTimelineEntry.PaymentEntry;
                    }
                    if (chargesToggle.isSelected()) {
                        return entry instanceof LoanTimelineEntry.ChargeEntry;
                    }
                    if (pendingToggle.isSelected()) {
                        return entry instanceof LoanTimelineEntry.ChargeEntry chargeEntry
                                && chargeEntry.charge().status() != LoanInterestChargeStatus.PAID;
                    }
                    return true;
                })
                .toList();
        timelineTable.setItems(FXCollections.observableArrayList(filtered));
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
