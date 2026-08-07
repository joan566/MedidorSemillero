package com.playground.fondoahorro.presentation.movement;

import com.playground.fondoahorro.application.movement.MovementService;
import com.playground.fondoahorro.domain.movement.MovementFilter;
import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementListItem;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.Labels;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/** Embedded in the person detail screen's "Movimientos" tab, scoped to a single person. Read-only. */
public class PersonMovementsTabController {

    private static final Color INCOME_ICON_COLOR = Color.web("#16A34A");
    private static final Color EXPENSE_ICON_COLOR = Color.web("#DC2626");

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
    private TableColumn<MovementListItem, MovementListItem> valueColumn;

    private final MovementService movementService = new MovementService(
            new JdbcMovementRepository(), new JdbcMovementTypeRepository(), new JdbcPersonRepository());

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

        valueColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        valueColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MovementListItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("value-income", "value-expense");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(MoneyFormatter.format(item.movement().amount()));
                getStyleClass().add(item.movement().kind() == MovementKind.INCOME ? "value-income" : "value-expense");
            }
        });

        table.setPlaceholder(EmptyStates.of(Icon.MOVEMENTS, "Aún no hay movimientos",
                "Los movimientos de esta persona aparecerán aquí."));
    }

    public void setPerson(Person person) {
        MovementFilter filter = new MovementFilter(null, null, null, null, null, person.id(), null);
        table.setItems(FXCollections.observableArrayList(movementService.list(filter)));
    }
}
