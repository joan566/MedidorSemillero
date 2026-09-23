package com.playground.fondoahorro.presentation.shell.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playground.fondoahorro.presentation.loan.controller.LoanDetailController;
import com.playground.fondoahorro.presentation.loan.controller.LoanListController;
import com.playground.fondoahorro.presentation.person.controller.PersonDetailController;
import com.playground.fondoahorro.presentation.person.controller.PersonListController;
import com.playground.fondoahorro.presentation.settlement.controller.SettlementDetailController;
import com.playground.fondoahorro.presentation.settlement.controller.SettlementListController;
import com.playground.fondoahorro.presentation.shared.Icons;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;

import java.io.IOException;

/**
 * Hosts the sidebar navigation and swaps the center content area.
 * Each nav handler currently shows a placeholder; later phases will replace
 * the placeholder with the real FXML view for that module.
 */
public class ShellController {

    private static final Logger log = LoggerFactory.getLogger(ShellController.class);
    private static final double NAV_ICON_SIZE = 17;

    @FXML
    private StackPane contentArea;
    @FXML
    private ToggleButton dashboardNav;
    @FXML
    private ToggleButton personsNav;
    @FXML
    private ToggleButton savingsNav;
    @FXML
    private ToggleButton loansNav;
    @FXML
    private ToggleButton birthdaysNav;
    @FXML
    private ToggleButton settlementsNav;
    @FXML
    private ToggleButton movementsNav;
    @FXML
    private ToggleButton settingsNav;

    @FXML
    private void initialize() {
        dashboardNav.setGraphic(Icons.node(Icon.DASHBOARD, NAV_ICON_SIZE));
        personsNav.setGraphic(Icons.node(Icon.PERSONS, NAV_ICON_SIZE));
        savingsNav.setGraphic(Icons.node(Icon.SAVINGS, NAV_ICON_SIZE));
        loansNav.setGraphic(Icons.node(Icon.LOANS, NAV_ICON_SIZE));
        birthdaysNav.setGraphic(Icons.node(Icon.BIRTHDAYS, NAV_ICON_SIZE));
        settlementsNav.setGraphic(Icons.node(Icon.SETTLEMENTS, NAV_ICON_SIZE));
        movementsNav.setGraphic(Icons.node(Icon.MOVEMENTS, NAV_ICON_SIZE));
        settingsNav.setGraphic(Icons.node(Icon.SETTINGS, NAV_ICON_SIZE));

        loadDashboard();
    }

    @FXML
    private void showDashboard() {
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            Parent view = new FXMLLoader(getClass().getResource("/fxml/dashboard/dashboard.fxml")).load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el dashboard", e);
            showPlaceholder("Dashboard");
        }
    }

    @FXML
    private void showPersons() {
        loadPersonList();
    }

    @FXML
    private void showSavings() {
        loadView("/fxml/savings/saving_list.fxml", "Ahorros");
    }

    @FXML
    private void showLoans() {
        loadLoanList();
    }

    @FXML
    private void showBirthdays() {
        loadView("/fxml/birthday/birthday_list.fxml", "Cumpleaños");
    }

    @FXML
    private void showSettlements() {
        loadSettlementList();
    }

    @FXML
    private void showMovements() {
        loadView("/fxml/movement/movement_list.fxml", "Movimientos");
    }

    @FXML
    private void showSettings() {
        loadView("/fxml/settings/settings.fxml", "Configuración");
    }

    private void showPlaceholder(String section) {
        Label label = new Label(section + "\n\nEsta sección se implementará en una fase siguiente.");
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: -fx-text-muted;");
        StackPane.setAlignment(label, Pos.CENTER);
        contentArea.getChildren().setAll(label);
    }

    private void loadPersonList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/person/person_list.fxml"));
            Parent view = loader.load();
            PersonListController controller = loader.getController();
            controller.setOnPersonSelected(this::loadPersonDetail);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el listado de personas", e);
            showPlaceholder("Personas");
        }
    }

    private void loadPersonDetail(long personId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/person/person_detail.fxml"));
            Parent view = loader.load();
            PersonDetailController controller = loader.getController();
            controller.setOnBack(this::loadPersonList);
            controller.setOnLoanSelected(loanId -> loadLoanDetail(loanId, () -> loadPersonDetail(personId)));
            controller.setOnSettlementYearSelected((pid, year) -> loadSettlementDetail(pid, year, () -> loadPersonDetail(personId)));
            controller.setPersonId(personId);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el detalle de la persona {}", personId, e);
            showPlaceholder("Personas");
        }
    }

    private void loadLoanList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loan/loan_list.fxml"));
            Parent view = loader.load();
            LoanListController controller = loader.getController();
            controller.setOnLoanSelected(loanId -> loadLoanDetail(loanId, this::loadLoanList));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el listado de préstamos", e);
            showPlaceholder("Préstamos");
        }
    }

    private void loadLoanDetail(long loanId, Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loan/loan_detail.fxml"));
            Parent view = loader.load();
            LoanDetailController controller = loader.getController();
            controller.setOnBack(onBack);
            controller.setLoanId(loanId);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el detalle del préstamo {}", loanId, e);
            showPlaceholder("Préstamos");
        }
    }

    private void loadSettlementList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settlement/settlement_list.fxml"));
            Parent view = loader.load();
            SettlementListController controller = loader.getController();
            controller.setOnPersonSelected((personId, year) -> loadSettlementDetail(personId, year, this::loadSettlementList));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el listado de liquidaciones", e);
            showPlaceholder("Liquidaciones");
        }
    }

    private void loadSettlementDetail(long personId, int year, Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settlement/settlement_detail.fxml"));
            Parent view = loader.load();
            SettlementDetailController controller = loader.getController();
            controller.setOnBack(onBack);
            controller.setContext(personId, year);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar el detalle de la liquidación de la persona {} para {}", personId, year, e);
            showPlaceholder("Liquidaciones");
        }
    }

    private void loadView(String fxmlPath, String sectionNameForErrors) {
        try {
            Parent view = new FXMLLoader(getClass().getResource(fxmlPath)).load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar la vista {}", fxmlPath, e);
            showPlaceholder(sectionNameForErrors);
        }
    }
}
