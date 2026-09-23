package com.playground.fondoahorro.presentation.savings.controller;

import com.playground.fondoahorro.domain.inputport.SavingService;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.entity.Saving;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSavingRepository;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.Labels;
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

import java.util.Objects;
import com.playground.fondoahorro.application.savings.service.SavingServiceImpl;

/** Embedded in the person detail screen's "Ahorros" tab, scoped to a single person. */
public class PersonSavingsTabController {

    private static final Logger log = LoggerFactory.getLogger(PersonSavingsTabController.class);

    @FXML
    private TableView<Saving> table;
    @FXML
    private TableColumn<Saving, String> dateColumn;
    @FXML
    private TableColumn<Saving, String> amountColumn;
    @FXML
    private TableColumn<Saving, String> methodColumn;
    @FXML
    private TableColumn<Saving, String> notesColumn;

    private final SavingService savingService = new SavingServiceImpl(
            new JdbcSavingRepository(), new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository());

    private Person person;
    private Runnable onChanged;

    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().date())));
        amountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().amount())));
        methodColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Labels.of(data.getValue().paymentMethod())));
        notesColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Objects.requireNonNullElse(data.getValue().notes(), "—")));
        table.setPlaceholder(EmptyStates.of(Icon.SAVINGS, "Aún no hay ahorros",
                "Cuando registres un ahorro para esta persona aparecerá aquí.", "+ Nuevo ahorro", this::handleNewSaving));

        table.setRowFactory(tv -> {
            TableRow<Saving> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    handleEditSaving(row.getItem());
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

    @FXML
    private void handleNewSaving() {
        SavingFormDialog.showForPerson(person).ifPresent(input -> {
            try {
                savingService.registerSaving(input.personId(), Money.of(input.amount()), input.date(), input.method(), input.notes());
                refresh();
                if (onChanged != null) {
                    onChanged.run();
                }
                Toast.show(table.getScene().getWindow(), "Ahorro registrado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar ahorro para la persona {}", person.id(), e);
                Dialogs.error("Por ahora no fue posible registrar el ahorro. Intenta nuevamente.");
            }
        });
    }

    private void handleEditSaving(Saving saving) {
        SavingFormDialog.showForEdit(saving).ifPresent(input -> {
            try {
                savingService.updateSaving(saving.id(), Money.of(input.amount()), input.date(), input.method(), input.notes());
                refresh();
                if (onChanged != null) {
                    onChanged.run();
                }
                Toast.show(table.getScene().getWindow(), "Ahorro actualizado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al actualizar ahorro para la persona {}", person.id(), e);
                Dialogs.error("Por ahora no fue posible actualizar el ahorro. Intenta nuevamente.");
            }
        });
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(savingService.historyForPerson(person.id())));
    }
}
