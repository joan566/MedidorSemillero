package com.playground.fondoahorro.presentation.movement;

import com.playground.fondoahorro.application.movement.MovementService;
import com.playground.fondoahorro.application.movement.MovementTypeService;
import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.Fund;
import com.playground.fondoahorro.domain.movement.MovementFilter;
import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementListItem;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.FilterOption;
import com.playground.fondoahorro.presentation.shared.Icons;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.Labels;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MovementListController {

    private static final Logger log = LoggerFactory.getLogger(MovementListController.class);

    private static final Color INCOME_ICON_COLOR = Color.web("#16A34A");
    private static final Color EXPENSE_ICON_COLOR = Color.web("#DC2626");

    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<FilterOption<MovementKind>> kindCombo;
    @FXML
    private ComboBox<FilterOption<Fund>> fundCombo;
    @FXML
    private ComboBox<FilterOption<PaymentMethod>> methodCombo;
    @FXML
    private ComboBox<FilterOption<Long>> personCombo;
    @FXML
    private ComboBox<FilterOption<Long>> typeCombo;
    @FXML
    private TableView<MovementListItem> table;
    @FXML
    private TableColumn<MovementListItem, String> dateColumn;
    @FXML
    private TableColumn<MovementListItem, String> kindColumn;
    @FXML
    private TableColumn<MovementListItem, String> categoryColumn;
    @FXML
    private TableColumn<MovementListItem, String> fundColumn;
    @FXML
    private TableColumn<MovementListItem, String> methodColumn;
    @FXML
    private TableColumn<MovementListItem, String> personColumn;
    @FXML
    private TableColumn<MovementListItem, MovementListItem> valueColumn;

    private final MovementService movementService = new MovementService(
            new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository());
    private final MovementTypeService movementTypeService = new MovementTypeService(new JdbcMovementTypeRepository());
    private final PersonService personService = new PersonService(new JdbcPersonRepository());

    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(DateFormatter.format(data.getValue().movement().date())));
        kindColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Labels.of(data.getValue().movement().kind())));
        kindColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                boolean isIncome = value.equals(Labels.of(MovementKind.INCOME));
                var icon = Icons.node(isIncome ? Icon.ARROW_UP : Icon.ARROW_DOWN, 12,
                        isIncome ? INCOME_ICON_COLOR : EXPENSE_ICON_COLOR);
                Label badge = Badges.of(value, isIncome ? BadgeStyle.SUCCESS : BadgeStyle.DANGER);
                HBox box = new HBox(6, icon, badge);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        categoryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().movementTypeName()));
        fundColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Labels.of(data.getValue().movement().fund())));
        methodColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Labels.of(data.getValue().movement().paymentMethod())));
        personColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Objects.requireNonNullElse(data.getValue().personName(), "—")));

        valueColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyObjectWrapper<>(data.getValue()));
        valueColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MovementListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("value-income", "value-expense");
                    return;
                }
                setText(MoneyFormatter.format(item.movement().amount()));
                getStyleClass().removeAll("value-income", "value-expense");
                getStyleClass().add(item.movement().kind() == MovementKind.INCOME ? "value-income" : "value-expense");
            }
        });

        table.setPlaceholder(EmptyStates.of(Icon.MOVEMENTS, "Aún no hay movimientos registrados",
                "Los movimientos que registres en la aplicación aparecerán aquí."));

        populateFilters();

        fromDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        toDatePicker.valueProperty().addListener((obs, o, n) -> refresh());
        kindCombo.valueProperty().addListener((obs, o, n) -> refresh());
        fundCombo.valueProperty().addListener((obs, o, n) -> refresh());
        methodCombo.valueProperty().addListener((obs, o, n) -> refresh());
        personCombo.valueProperty().addListener((obs, o, n) -> refresh());
        typeCombo.valueProperty().addListener((obs, o, n) -> refresh());

        refresh();
    }

    private void populateFilters() {
        kindCombo.setItems(FXCollections.observableArrayList(
                new FilterOption<>("Todos", null),
                new FilterOption<>("Ingreso", MovementKind.INCOME),
                new FilterOption<>("Egreso", MovementKind.EXPENSE)));
        kindCombo.getSelectionModel().selectFirst();

        fundCombo.setItems(FXCollections.observableArrayList(
                new FilterOption<>("Todos los fondos", null),
                new FilterOption<>(Labels.of(Fund.SAVINGS), Fund.SAVINGS),
                new FilterOption<>(Labels.of(Fund.BIRTHDAY), Fund.BIRTHDAY)));
        fundCombo.getSelectionModel().selectFirst();

        methodCombo.setItems(FXCollections.observableArrayList(
                new FilterOption<>("Todos los medios", null),
                new FilterOption<>(Labels.of(PaymentMethod.CASH), PaymentMethod.CASH),
                new FilterOption<>(Labels.of(PaymentMethod.TRANSFER), PaymentMethod.TRANSFER)));
        methodCombo.getSelectionModel().selectFirst();

        List<FilterOption<Long>> personOptions = new ArrayList<>();
        personOptions.add(new FilterOption<>("Todas las personas", null));
        for (Person person : personService.list(null, true)) {
            personOptions.add(new FilterOption<>(person.name(), person.id()));
        }
        personCombo.setItems(FXCollections.observableArrayList(personOptions));
        personCombo.getSelectionModel().selectFirst();

        List<FilterOption<Long>> typeOptions = new ArrayList<>();
        typeOptions.add(new FilterOption<>("Todas las categorías", null));
        for (MovementType type : movementTypeService.listAll(true)) {
            typeOptions.add(new FilterOption<>(type.name() + " (" + Labels.of(type.kind()) + ")", type.id()));
        }
        typeCombo.setItems(FXCollections.observableArrayList(typeOptions));
        typeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleNewIncome() {
        handleNewMovement(MovementKind.INCOME);
    }

    @FXML
    private void handleNewExpense() {
        handleNewMovement(MovementKind.EXPENSE);
    }

    private void handleNewMovement(MovementKind kind) {
        MovementFormDialog.showForCreate(kind).ifPresent(input -> {
            try {
                movementService.registerMovement(input.movementTypeId(), input.fund(), input.method(),
                        Money.of(input.amount()), input.date(), input.personId(), input.notes());
                refresh();
                Toast.show(table.getScene().getWindow(), "Movimiento registrado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar movimiento", e);
                Dialogs.error("Por ahora no fue posible registrar el movimiento. Intenta nuevamente.");
            }
        });
    }

    @FXML
    private void handleClearFilters() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        kindCombo.getSelectionModel().selectFirst();
        fundCombo.getSelectionModel().selectFirst();
        methodCombo.getSelectionModel().selectFirst();
        personCombo.getSelectionModel().selectFirst();
        typeCombo.getSelectionModel().selectFirst();
        refresh();
    }

    private void refresh() {
        MovementFilter filter = new MovementFilter(
                fromDatePicker.getValue(),
                toDatePicker.getValue(),
                valueOf(kindCombo),
                valueOf(fundCombo),
                valueOf(methodCombo),
                valueOf(personCombo),
                valueOf(typeCombo));
        table.setItems(FXCollections.observableArrayList(movementService.list(filter)));
    }

    private static <T> T valueOf(ComboBox<FilterOption<T>> combo) {
        FilterOption<T> selected = combo.getValue();
        return selected == null ? null : selected.value();
    }
}
