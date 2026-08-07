package com.playground.fondoahorro.presentation.person;

import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.person.PersonSummary;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.presentation.loan.PersonLoansTabController;
import com.playground.fondoahorro.presentation.movement.PersonMovementsTabController;
import com.playground.fondoahorro.presentation.savings.PersonSavingsTabController;
import com.playground.fondoahorro.presentation.settlement.PersonSettlementsTabController;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PersonDetailController {

    private static final Logger log = LoggerFactory.getLogger(PersonDetailController.class);

    @FXML
    private Label nameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label birthDateLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label savingsValueLabel;
    @FXML
    private Label debtValueLabel;
    @FXML
    private Button toggleActiveButton;
    @FXML
    private StackPane savingsTabContent;
    @FXML
    private StackPane loansTabContent;
    @FXML
    private StackPane settlementsTabContent;
    @FXML
    private StackPane movementsTabContent;

    private final PersonService personService = new PersonService(new JdbcPersonRepository());
    private long personId;
    private Runnable onBack;
    private Consumer<Long> onLoanSelected;
    private BiConsumer<Long, Integer> onSettlementYearSelected;
    private PersonMovementsTabController movementsTabController;

    public void setPersonId(long personId) {
        this.personId = personId;
        loadSavingsTab();
        loadLoansTab();
        loadSettlementsTab();
        loadMovementsTab();
        load();
    }

    private void loadSavingsTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/savings/person_savings_tab.fxml"));
            Parent view = loader.load();
            PersonSavingsTabController controller = loader.getController();
            controller.setOnChanged(this::load);
            controller.setPerson(personService.findById(personId).orElseThrow());
            savingsTabContent.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar la pestaña de ahorros para la persona {}", personId, e);
        }
    }

    private void loadLoansTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loan/person_loans_tab.fxml"));
            Parent view = loader.load();
            PersonLoansTabController controller = loader.getController();
            controller.setOnChanged(this::load);
            controller.setOnLoanSelected(onLoanSelected);
            controller.setPerson(personService.findById(personId).orElseThrow());
            loansTabContent.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar la pestaña de préstamos para la persona {}", personId, e);
        }
    }

    private void loadSettlementsTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settlement/person_settlements_tab.fxml"));
            Parent view = loader.load();
            PersonSettlementsTabController controller = loader.getController();
            controller.setOnYearSelected(year -> {
                if (onSettlementYearSelected != null) {
                    onSettlementYearSelected.accept(personId, year);
                }
            });
            controller.setPerson(personService.findById(personId).orElseThrow());
            settlementsTabContent.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar la pestaña de liquidaciones para la persona {}", personId, e);
        }
    }

    private void loadMovementsTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/movement/person_movements_tab.fxml"));
            Parent view = loader.load();
            movementsTabController = loader.getController();
            movementsTabController.setPerson(personService.findById(personId).orElseThrow());
            movementsTabContent.getChildren().setAll(view);
        } catch (IOException e) {
            log.error("Error al cargar la pestaña de movimientos para la persona {}", personId, e);
        }
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setOnLoanSelected(Consumer<Long> onLoanSelected) {
        this.onLoanSelected = onLoanSelected;
    }

    public void setOnSettlementYearSelected(BiConsumer<Long, Integer> onSettlementYearSelected) {
        this.onSettlementYearSelected = onSettlementYearSelected;
    }

    private void load() {
        Person person = personService.findById(personId)
                .orElseThrow(() -> new IllegalStateException("La persona no existe."));
        PersonSummary summary = personService.getSummary(personId);

        nameLabel.setText(person.name());
        statusLabel.setText(person.active() ? "Activa" : "Inactiva");
        statusLabel.getStyleClass().setAll(person.active() ? "value-income" : "value-expense");
        birthDateLabel.setText(DateFormatter.format(person.birthDate()));
        phoneLabel.setText(Objects.requireNonNullElse(person.phone(), "—"));
        savingsValueLabel.setText(MoneyFormatter.format(summary.totalSavings()));
        debtValueLabel.setText(MoneyFormatter.format(summary.outstandingDebt()));
        toggleActiveButton.setText(person.active() ? "Desactivar" : "Activar");

        if (movementsTabController != null) {
            movementsTabController.setPerson(person);
        }
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void handleEdit() {
        Person person = personService.findById(personId).orElseThrow();
        PersonFormDialog.showForEdit(person).ifPresent(input -> {
            try {
                personService.updatePerson(personId, input.name(), input.birthDate(), input.phone());
                load();
                Toast.show(nameLabel.getScene().getWindow(), "Persona actualizada.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al actualizar persona {}", personId, e);
                Dialogs.error("Por ahora no fue posible actualizar la persona. Intenta nuevamente.");
            }
        });
    }

    @FXML
    private void handleToggleActive() {
        Person person = personService.findById(personId).orElseThrow();
        boolean newActive = !person.active();
        String message = newActive
                ? "¿Activar a " + person.name() + "? Volverá a aparecer en los listados activos."
                : "¿Desactivar a " + person.name() + "? Ya no aparecerá en los listados activos, pero se conservará su historial.";

        if (Dialogs.confirm("Confirmar", message, "Cancelar", newActive ? "Activar" : "Desactivar")) {
            toggleActiveButton.setDisable(true);
            try {
                personService.setActive(personId, newActive);
                load();
                Toast.show(toggleActiveButton.getScene().getWindow(), newActive ? "Persona activada." : "Persona desactivada.");
            } catch (Exception e) {
                log.error("Error al cambiar estado de persona {}", personId, e);
                Dialogs.error("Por ahora no fue posible actualizar el estado. Intenta nuevamente.");
            } finally {
                toggleActiveButton.setDisable(false);
            }
        }
    }
}
