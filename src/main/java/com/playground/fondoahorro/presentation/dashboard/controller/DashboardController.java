package com.playground.fondoahorro.presentation.dashboard.controller;

import com.playground.fondoahorro.domain.inputport.BirthdayGiftService;
import com.playground.fondoahorro.domain.inputport.DashboardService;
import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.MovementService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.vo.UpcomingBirthday;
import com.playground.fondoahorro.domain.vo.DashboardSnapshot;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.vo.MovementListItem;
import com.playground.fondoahorro.infrastructure.repository.JdbcBirthdayGiftRepository;
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
import com.playground.fondoahorro.presentation.shared.Icons;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;
import com.playground.fondoahorro.application.loan.service.LoanServiceImpl;
import com.playground.fondoahorro.application.movement.service.MovementServiceImpl;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;
import com.playground.fondoahorro.application.birthday.service.BirthdayGiftServiceImpl;
import com.playground.fondoahorro.application.dashboard.service.DashboardServiceImpl;

public class DashboardController {

    private static final Color HEADER_ICON_COLOR = Color.web("#4B5563");
    private static final Color DIRECTION_ICON_COLOR_INCOME = Color.web("#16A34A");
    private static final Color DIRECTION_ICON_COLOR_EXPENSE = Color.web("#DC2626");

    @FXML
    private Label greetingLabel;
    @FXML
    private Label savingsHeaderLabel;
    @FXML
    private Label savingsTotalLabel;
    @FXML
    private Label savingsCashLabel;
    @FXML
    private Label savingsTransferLabel;
    @FXML
    private Label birthdayHeaderLabel;
    @FXML
    private Label birthdayTotalLabel;
    @FXML
    private Label birthdayCashLabel;
    @FXML
    private Label birthdayTransferLabel;
    @FXML
    private Label loansHeaderLabel;
    @FXML
    private Label loanedMoneyLabel;
    @FXML
    private Label outstandingDebtLabel;
    @FXML
    private Label activeLoanCountLabel;
    @FXML
    private Label personsHeaderLabel;
    @FXML
    private Label personCountLabel;
    @FXML
    private VBox birthdaysContainer;
    @FXML
    private VBox activityContainer;

    private final DashboardService dashboardService = new DashboardServiceImpl(
            new MovementServiceImpl(new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository()),
            new LoanServiceImpl(new JdbcLoanRepository(), new JdbcLoanPaymentRepository(), new JdbcLoanInterestChargeRepository(),
                    new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
                    new AppSettingsServiceImpl(new JdbcAppSettingsRepository())),
            new PersonServiceImpl(new JdbcPersonRepository()),
            new BirthdayGiftServiceImpl(new JdbcBirthdayGiftRepository(), new JdbcMovementRepository(),
                    new JdbcMovementTypeRepository(), new JdbcPersonRepository(),
                    new AppSettingsServiceImpl(new JdbcAppSettingsRepository())));

    @FXML
    private void initialize() {
        greetingLabel.setText(greeting());
        savingsHeaderLabel.setGraphic(Icons.node(Icon.SAVINGS, 16, HEADER_ICON_COLOR));
        birthdayHeaderLabel.setGraphic(Icons.node(Icon.BIRTHDAYS, 16, HEADER_ICON_COLOR));
        loansHeaderLabel.setGraphic(Icons.node(Icon.LOANS, 16, HEADER_ICON_COLOR));
        personsHeaderLabel.setGraphic(Icons.node(Icon.PERSONS, 16, HEADER_ICON_COLOR));

        refresh();
    }

    public void refresh() {
        DashboardSnapshot snapshot = dashboardService.getSnapshot();

        savingsTotalLabel.setText(MoneyFormatter.format(snapshot.fundBalances().savingsTotal()));
        savingsCashLabel.setText(MoneyFormatter.format(snapshot.fundBalances().savingsCash()));
        savingsTransferLabel.setText(MoneyFormatter.format(snapshot.fundBalances().savingsTransfer()));

        birthdayTotalLabel.setText(MoneyFormatter.format(snapshot.fundBalances().birthdayTotal()));
        birthdayCashLabel.setText(MoneyFormatter.format(snapshot.fundBalances().birthdayCash()));
        birthdayTransferLabel.setText(MoneyFormatter.format(snapshot.fundBalances().birthdayTransfer()));

        loanedMoneyLabel.setText(MoneyFormatter.format(snapshot.loanedMoney()));
        outstandingDebtLabel.setText(MoneyFormatter.format(snapshot.outstandingDebt()));
        activeLoanCountLabel.setText(String.valueOf(snapshot.activeLoanCount()));

        personCountLabel.setText(String.valueOf(snapshot.activePersonCount()));

        birthdaysContainer.getChildren().clear();
        if (snapshot.upcomingBirthdays().isEmpty()) {
            birthdaysContainer.getChildren().add(new Label("Aún no hay personas activas registradas."));
        } else {
            for (UpcomingBirthday item : snapshot.upcomingBirthdays()) {
                birthdaysContainer.getChildren().add(buildBirthdayRow(item));
            }
        }

        activityContainer.getChildren().clear();
        if (snapshot.recentActivity().isEmpty()) {
            activityContainer.getChildren().add(new Label("Aún no hay movimientos registrados."));
        } else {
            for (MovementListItem item : snapshot.recentActivity()) {
                activityContainer.getChildren().add(buildActivityRow(item));
            }
        }
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Buenos días";
        } else if (hour < 19) {
            return "Buenas tardes";
        } else {
            return "Buenas noches";
        }
    }

    private HBox buildBirthdayRow(UpcomingBirthday item) {
        Label name = new Label(item.person().name());
        name.setStyle("-fx-font-weight: bold;");
        Label date = new Label(DateFormatter.formatDayMonth(item.occurrenceDate()));
        date.getStyleClass().add("metric-label");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = item.giftGiven()
                ? Badges.of("Entregado", BadgeStyle.SUCCESS)
                : Badges.of("Pendiente", BadgeStyle.WARNING);

        HBox row = new HBox(10, name, date, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildActivityRow(MovementListItem item) {
        boolean isIncome = item.movement().kind() == MovementKind.INCOME;
        var icon = Icons.node(isIncome ? Icon.ARROW_UP : Icon.ARROW_DOWN, 14,
                isIncome ? DIRECTION_ICON_COLOR_INCOME : DIRECTION_ICON_COLOR_EXPENSE);
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setStyle("-fx-background-color: " + (isIncome ? "-fx-success-subtle" : "-fx-danger-subtle") + "; "
                + "-fx-background-radius: 8; -fx-padding: 8; -fx-min-width: 30; -fx-min-height: 30;");

        Label category = new Label(item.movementTypeName());
        category.setStyle("-fx-font-weight: 600;");
        Label person = new Label(Objects.requireNonNullElse(item.personName(), "—"));
        person.getStyleClass().add("metric-label");
        VBox textBox = new VBox(1, category, person);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(relativeDate(item.movement().date()));
        date.getStyleClass().add("metric-label");
        Label value = new Label((isIncome ? "+" : "-") + MoneyFormatter.format(item.movement().amount()));
        value.getStyleClass().add(isIncome ? "value-income" : "value-expense");
        value.setStyle("-fx-font-weight: bold;");
        VBox valueBox = new VBox(1, date, value);
        valueBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, iconBox, textBox, spacer, valueBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 6 0;");
        return row;
    }

    private static String relativeDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Hoy";
        }
        if (date.equals(today.minusDays(1))) {
            return "Ayer";
        }
        return DateFormatter.format(date);
    }
}
