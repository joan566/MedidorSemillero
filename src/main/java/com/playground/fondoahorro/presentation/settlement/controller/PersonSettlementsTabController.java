package com.playground.fondoahorro.presentation.settlement.controller;

import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.inputport.SettlementService;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.entity.Settlement;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSavingRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcAppSettingsRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSettlementRepository;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;
import com.playground.fondoahorro.application.settlement.service.SettlementServiceImpl;

/** Embedded in the person detail screen's "Liquidaciones" tab: only shows settlements already prepared. */
public class PersonSettlementsTabController {

    private static final DateTimeFormatter PREPARED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private TableView<Settlement> table;
    @FXML
    private TableColumn<Settlement, String> yearColumn;
    @FXML
    private TableColumn<Settlement, String> savingsColumn;
    @FXML
    private TableColumn<Settlement, String> interestColumn;
    @FXML
    private TableColumn<Settlement, String> totalColumn;
    @FXML
    private TableColumn<Settlement, String> preparedAtColumn;

    private final SettlementService settlementService = new SettlementServiceImpl(
            new JdbcSettlementRepository(), new JdbcSavingRepository(), new JdbcPersonRepository(),
            new AppSettingsServiceImpl(new JdbcAppSettingsRepository()));

    private Person person;
    private Consumer<Integer> onYearSelected;

    @FXML
    private void initialize() {
        yearColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().year())));
        savingsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().savingsTotal())));
        interestColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().interestAmount())));
        totalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().totalAmount())));
        preparedAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().preparedAt().format(PREPARED_AT_FORMAT)));

        table.setPlaceholder(EmptyStates.of(Icon.SETTLEMENTS, "Aún no hay liquidaciones preparadas",
                "Las liquidaciones que prepares para esta persona aparecerán aquí.", "Preparar liquidación", this::handlePrepareForYear));
        table.setRowFactory(tv -> {
            TableRow<Settlement> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && onYearSelected != null) {
                    onYearSelected.accept(row.getItem().year());
                }
            });
            return row;
        });
    }

    public void setPerson(Person person) {
        this.person = person;
        refresh();
    }

    public void setOnYearSelected(Consumer<Integer> onYearSelected) {
        this.onYearSelected = onYearSelected;
    }

    @FXML
    private void handlePrepareForYear() {
        YearPickerDialog.show("Preparar liquidación").ifPresent(year -> {
            if (onYearSelected != null) {
                onYearSelected.accept(year);
            }
        });
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(settlementService.historyForPerson(person.id())));
    }
}
