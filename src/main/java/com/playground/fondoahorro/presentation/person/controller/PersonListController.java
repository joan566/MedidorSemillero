package com.playground.fondoahorro.presentation.person.controller;

import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.vo.PersonSummary;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;

public class PersonListController {

    private static final Logger log = LoggerFactory.getLogger(PersonListController.class);

    @FXML
    private TextField searchField;
    @FXML
    private CheckBox includeInactiveCheckBox;
    @FXML
    private TableView<PersonSummary> table;
    @FXML
    private TableColumn<PersonSummary, String> nameColumn;
    @FXML
    private TableColumn<PersonSummary, String> birthDateColumn;
    @FXML
    private TableColumn<PersonSummary, String> phoneColumn;
    @FXML
    private TableColumn<PersonSummary, String> savingsColumn;
    @FXML
    private TableColumn<PersonSummary, String> debtColumn;
    @FXML
    private TableColumn<PersonSummary, String> statusColumn;

    private final PersonService personService = new PersonServiceImpl(new JdbcPersonRepository());
    private Consumer<Long> onPersonSelected;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().person().name()));
        birthDateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().person().birthDate())));
        phoneColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(Objects.requireNonNullElse(data.getValue().person().phone(), "—")));
        savingsColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().totalSavings())));
        debtColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().outstandingDebt())));
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().person().active() ? "Activa" : "Inactiva"));
        statusColumn.setCellFactory(Badges.cellFactory(status -> status.equals("Activa") ? BadgeStyle.SUCCESS : BadgeStyle.NEUTRAL));

        table.setPlaceholder(EmptyStates.of(Icon.PERSONS, "Aún no hay personas registradas",
                "Cuando registres una persona aparecerá aquí.", "+ Nueva persona", this::handleNewPerson));
        table.setRowFactory(tv -> {
            TableRow<PersonSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && onPersonSelected != null) {
                    onPersonSelected.accept(row.getItem().person().id());
                }
            });
            return row;
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh());
        includeInactiveCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> refresh());

        refresh();
    }

    public void setOnPersonSelected(Consumer<Long> handler) {
        this.onPersonSelected = handler;
    }

    public void refresh() {
        List<PersonSummary> results = personService.listWithSummary(searchField.getText(), includeInactiveCheckBox.isSelected());
        table.setItems(FXCollections.observableArrayList(results));
    }

    @FXML
    private void handleNewPerson() {
        PersonFormDialog.showForCreate().ifPresent(input -> {
            try {
                personService.createPerson(input.name(), input.birthDate(), input.phone());
                refresh();
                Toast.show(table.getScene().getWindow(), "Persona registrada.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar persona", e);
                Dialogs.error("Por ahora no fue posible registrar la persona. Intenta nuevamente.");
            }
        });
    }
}
