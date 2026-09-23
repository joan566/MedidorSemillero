package com.playground.fondoahorro.presentation.settlement.controller;

import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.inputport.SettlementService;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.SettlementRow;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSavingRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcAppSettingsRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSettlementRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.function.BiConsumer;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;
import com.playground.fondoahorro.application.settlement.service.SettlementServiceImpl;

public class SettlementListController {

    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private TableView<SettlementRow> table;
    @FXML
    private TableColumn<SettlementRow, String> personColumn;
    @FXML
    private TableColumn<SettlementRow, String> savingsColumn;
    @FXML
    private TableColumn<SettlementRow, String> interestColumn;
    @FXML
    private TableColumn<SettlementRow, String> totalColumn;
    @FXML
    private TableColumn<SettlementRow, String> statusColumn;
    @FXML
    private Label yearTotalLabel;

    private final SettlementService settlementService = new SettlementServiceImpl(
            new JdbcSettlementRepository(), new JdbcSavingRepository(), new JdbcPersonRepository(),
            new AppSettingsServiceImpl(new JdbcAppSettingsRepository()));

    private BiConsumer<Long, Integer> onPersonSelected;

    @FXML
    private void initialize() {
        int currentYear = LocalDate.now().getYear();
        var years = FXCollections.<Integer>observableArrayList();
        for (int year = currentYear; year >= currentYear - 9; year--) {
            years.add(year);
        }
        yearCombo.setItems(years);
        yearCombo.getSelectionModel().selectFirst();
        yearCombo.valueProperty().addListener((obs, oldVal, newVal) -> refresh());

        personColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().person().name()));
        savingsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().calculated().savingsTotal())));
        interestColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().calculated().interestAmount())));
        totalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(MoneyFormatter.format(data.getValue().calculated().totalAmount())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().prepared() ? "Preparada" : "Pendiente de preparar"));
        statusColumn.setCellFactory(Badges.cellFactory(status -> status.equals("Preparada") ? BadgeStyle.SUCCESS : BadgeStyle.WARNING));

        table.setPlaceholder(EmptyStates.of(Icon.SETTLEMENTS, "Aún no hay personas activas registradas",
                "Cuando registres una persona activa aparecerá aquí."));
        table.setRowFactory(tv -> {
            TableRow<SettlementRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && onPersonSelected != null) {
                    onPersonSelected.accept(row.getItem().person().id(), yearCombo.getValue());
                }
            });
            return row;
        });

        refresh();
    }

    public void setOnPersonSelected(BiConsumer<Long, Integer> handler) {
        this.onPersonSelected = handler;
    }

    private void refresh() {
        Integer year = yearCombo.getValue();
        if (year == null) {
            return;
        }
        var rows = settlementService.listForYear(year);
        table.setItems(FXCollections.observableArrayList(rows));
        Money yearTotal = rows.stream().map(row -> row.calculated().totalAmount()).reduce(Money.ZERO, Money::plus);
        yearTotalLabel.setText(MoneyFormatter.format(yearTotal));
    }
}
