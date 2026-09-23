package com.playground.fondoahorro.presentation.settings.controller;

import com.playground.fondoahorro.application.importing.service.ExcelTemplateGenerator;
import com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser;
import com.playground.fondoahorro.application.importing.dto.ImportError;
import com.playground.fondoahorro.application.importing.dto.ImportExecutionException;
import com.playground.fondoahorro.application.importing.dto.ImportPlan;
import com.playground.fondoahorro.application.importing.service.ImportService;
import com.playground.fondoahorro.application.importing.dto.ImportValidationResult;
import com.playground.fondoahorro.application.importing.service.ImportValidator;
import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.MovementTypeService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.SavingService;
import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.infrastructure.adapter.DatabaseBackup;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanInterestChargeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanPaymentRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcLoanRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcSavingRepository;
import com.playground.fondoahorro.infrastructure.repository.JdbcAppSettingsRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.MoneyTextFields;
import com.playground.fondoahorro.presentation.shared.PercentageFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.application.person.service.PersonServiceImpl;
import com.playground.fondoahorro.application.loan.service.LoanServiceImpl;
import com.playground.fondoahorro.application.movement.service.MovementTypeServiceImpl;
import com.playground.fondoahorro.application.savings.service.SavingServiceImpl;
import com.playground.fondoahorro.application.settings.service.AppSettingsServiceImpl;
import com.playground.fondoahorro.application.importing.service.ImportServiceImpl;

public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @FXML
    private TableView<MovementType> incomeTable;
    @FXML
    private TableColumn<MovementType, String> incomeNameColumn;
    @FXML
    private TableColumn<MovementType, String> incomeOriginColumn;
    @FXML
    private TableColumn<MovementType, String> incomeStatusColumn;
    @FXML
    private TableColumn<MovementType, MovementType> incomeActionsColumn;

    @FXML
    private TableView<MovementType> expenseTable;
    @FXML
    private TableColumn<MovementType, String> expenseNameColumn;
    @FXML
    private TableColumn<MovementType, String> expenseOriginColumn;
    @FXML
    private TableColumn<MovementType, String> expenseStatusColumn;
    @FXML
    private TableColumn<MovementType, MovementType> expenseActionsColumn;
    @FXML
    private TextField loanInterestField;
    @FXML
    private TextField giftAmountField;
    @FXML
    private TextField settlementInterestField;

    private final MovementTypeService movementTypeService = new MovementTypeServiceImpl(new JdbcMovementTypeRepository());
    private final AppSettingsService appSettingsService = new AppSettingsServiceImpl(new JdbcAppSettingsRepository());
    private final PersonService personService = new PersonServiceImpl(new JdbcPersonRepository());
    private final SavingService savingService = new SavingServiceImpl(new JdbcSavingRepository(), new JdbcMovementRepository(),
            new JdbcMovementTypeRepository(), new JdbcPersonRepository());
    private final LoanService loanService = new LoanServiceImpl(new JdbcLoanRepository(), new JdbcLoanPaymentRepository(),
            new JdbcLoanInterestChargeRepository(), new JdbcMovementRepository(), new JdbcMovementTypeRepository(),
            new JdbcPersonRepository(), appSettingsService);
    private final ImportService importService = new ImportServiceImpl(personService, savingService, loanService);

    @FXML
    private void initialize() {
        MoneyTextFields.attachLiveFormatting(giftAmountField);

        configureTable(incomeTable, incomeNameColumn, incomeOriginColumn, incomeStatusColumn, incomeActionsColumn);
        configureTable(expenseTable, expenseNameColumn, expenseOriginColumn, expenseStatusColumn, expenseActionsColumn);
        refresh();
        loadFundSettings();
    }

    private void loadFundSettings() {
        loanInterestField.setText(PercentageFormatter.format(appSettingsService.getLoanInterestRateBps()));
        giftAmountField.setText(appSettingsService.getBirthdayGiftDefaultAmount().toPesos().toBigInteger().toString());
        settlementInterestField.setText(PercentageFormatter.format(appSettingsService.getSettlementInterestRateBps()));
    }

    @FXML
    private void handleSaveLoanInterest(ActionEvent event) {
        Optional<Integer> bps = PercentageFormatter.parseToBasisPoints(loanInterestField.getText());
        if (bps.isEmpty() || bps.get() < 0) {
            Dialogs.error("Ingresa una tasa de interés válida (por ejemplo, 3).");
            return;
        }
        try {
            appSettingsService.setLoanInterestRateBps(bps.get());
            Toast.show(((Node) event.getSource()).getScene().getWindow(), "Tasa de interés de préstamos actualizada.");
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar la tasa de interés de préstamos", e);
            Dialogs.error("Por ahora no fue posible guardar la configuración. Intenta nuevamente.");
        }
    }

    @FXML
    private void handleSaveGiftAmount(ActionEvent event) {
        Optional<BigDecimal> amount = MoneyFormatter.parse(giftAmountField.getText());
        if (amount.isEmpty() || amount.get().signum() <= 0) {
            Dialogs.error("Ingresa un valor mayor a $0.");
            return;
        }
        try {
            appSettingsService.setBirthdayGiftDefaultAmount(Money.of(amount.get()));
            Toast.show(((Node) event.getSource()).getScene().getWindow(), "Valor del regalo de cumpleaños actualizado.");
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar el valor del regalo de cumpleaños", e);
            Dialogs.error("Por ahora no fue posible guardar la configuración. Intenta nuevamente.");
        }
    }

    @FXML
    private void handleSaveSettlementInterest(ActionEvent event) {
        Optional<Integer> bps = PercentageFormatter.parseToBasisPoints(settlementInterestField.getText());
        if (bps.isEmpty() || bps.get() < 0) {
            Dialogs.error("Ingresa una tasa de interés válida (por ejemplo, 3).");
            return;
        }
        try {
            appSettingsService.setSettlementInterestRateBps(bps.get());
            Toast.show(((Node) event.getSource()).getScene().getWindow(), "Tasa de interés de liquidación actualizada.");
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar la tasa de interés de liquidación", e);
            Dialogs.error("Por ahora no fue posible guardar la configuración. Intenta nuevamente.");
        }
    }

    @FXML
    private void handleCreateBackup(ActionEvent event) {
        Window window = ((Node) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar copia de seguridad");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"));
        fileChooser.setInitialFileName("fondo-ahorro-" + timestamp + ".db");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos (*.db)", "*.db"));

        File destination = fileChooser.showSaveDialog(window);
        if (destination == null) {
            return;
        }
        try {
            DatabaseBackup.copyTo(destination.toPath());
            Dialogs.info("Copia de seguridad creada", "La copia se guardó en:\n" + destination.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error al crear la copia de seguridad", e);
            Dialogs.error("Por ahora no fue posible crear la copia de seguridad. Intenta nuevamente.");
        }
    }

    @FXML
    private void handleRestoreBackup(ActionEvent event) {
        Window window = ((Node) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar copia de seguridad");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos (*.db)", "*.db"));

        File source = fileChooser.showOpenDialog(window);
        if (source == null) {
            return;
        }

        boolean confirmed = Dialogs.confirm("Restaurar copia de seguridad",
                "Esto reemplazará TODOS los datos actuales por los de la copia seleccionada. Esta acción no se puede deshacer.\n\n"
                        + "La aplicación se cerrará después de restaurar; deberás abrirla de nuevo.\n\n¿Continuar?",
                "Cancelar", "Restaurar copia");
        if (!confirmed) {
            return;
        }

        try {
            DatabaseBackup.restoreFrom(source.toPath());
            Dialogs.info("Copia restaurada",
                    "La copia de seguridad se restauró correctamente. La aplicación se cerrará ahora; ábrela de nuevo para continuar.");
            Platform.exit();
        } catch (IOException e) {
            log.error("Error al restaurar la copia de seguridad", e);
            Dialogs.error("No fue posible restaurar la copia de seguridad. Verifica que el archivo sea válido e intenta nuevamente.");
        }
    }

    @FXML
    private void handleDownloadTemplate(ActionEvent event) {
        Window window = ((Node) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar plantilla de importación");
        fileChooser.setInitialFileName("plantilla-importacion-fondo-ahorro.xlsx");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        File destination = fileChooser.showSaveDialog(window);
        if (destination == null) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(destination)) {
            ExcelTemplateGenerator.write(out);
            Dialogs.info("Plantilla generada", "La plantilla se guardó en:\n" + destination.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error al generar la plantilla de importación", e);
            Dialogs.error("Por ahora no fue posible generar la plantilla. Intenta nuevamente.");
        }
    }

    @FXML
    private void handleImportFromExcel(ActionEvent event) {
        Window window = ((Node) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo de importación");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        File source = fileChooser.showOpenDialog(window);
        if (source == null) {
            return;
        }

        ExcelWorkbookParser.RawWorkbook raw;
        try (FileInputStream in = new FileInputStream(source)) {
            raw = ExcelWorkbookParser.parse(in);
        } catch (IOException e) {
            log.error("Error al leer el archivo de importación", e);
            Dialogs.error("No fue posible leer el archivo. Verifica que sea un Excel (.xlsx) válido.");
            return;
        }

        ImportValidationResult result = ImportValidator.validate(raw);
        if (!result.isValid()) {
            showValidationErrors(result.errors());
            return;
        }

        ImportPlan plan = result.plan().orElseThrow();
        String summary = "Se van a crear:\n"
                + "- " + plan.persons().size() + " persona(s)\n"
                + "- " + plan.savings().size() + " ahorro(s)\n"
                + "- " + plan.loans().size() + " préstamo(s)\n"
                + "- " + plan.payments().size() + " pago(s)\n\n"
                + "Se recomienda sacar una copia de seguridad antes de continuar (arriba en esta pantalla).\n\n¿Continuar con la importación?";
        if (!Dialogs.confirm("Confirmar importación", summary, "Cancelar", "Importar")) {
            return;
        }

        try {
            ImportService.ImportSummary executed = importService.execute(plan);
            Dialogs.info("Importación completa",
                    "Se crearon " + executed.personsCreated() + " persona(s), " + executed.savingsCreated() + " ahorro(s), "
                            + executed.loansCreated() + " préstamo(s) y " + executed.paymentsCreated() + " pago(s).");
        } catch (ImportExecutionException e) {
            Dialogs.error("Hoja " + e.sheet() + ", fila " + e.row() + ": " + e.getMessage()
                    + "\n\nNo se guardó ningún dato — corrige el archivo e intenta de nuevo.");
        } catch (Exception e) {
            log.error("Error al ejecutar la importación", e);
            Dialogs.error("Por ahora no fue posible completar la importación. Intenta nuevamente.");
        }
    }

    private void showValidationErrors(List<ImportError> errors) {
        StringBuilder message = new StringBuilder("El archivo tiene ").append(errors.size())
                .append(" error(es). No se guardó ningún dato.\n\n");
        int shown = Math.min(errors.size(), 20);
        for (int i = 0; i < shown; i++) {
            ImportError error = errors.get(i);
            message.append("- ").append(error.sheet()).append(" fila ").append(error.row())
                    .append(": ").append(error.message()).append("\n");
        }
        if (errors.size() > shown) {
            message.append("... y ").append(errors.size() - shown).append(" más.");
        }
        Dialogs.error(message.toString());
    }

    private void configureTable(TableView<MovementType> table,
                                 TableColumn<MovementType, String> nameColumn,
                                 TableColumn<MovementType, String> originColumn,
                                 TableColumn<MovementType, String> statusColumn,
                                 TableColumn<MovementType, MovementType> actionsColumn) {
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        originColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isSystemType() ? "Sistema" : "Personalizado"));
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().active() ? "Activo" : "Inactivo"));
        statusColumn.setCellFactory(Badges.cellFactory(status -> status.equals("Activo") ? BadgeStyle.SUCCESS : BadgeStyle.NEUTRAL));

        actionsColumn.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyObjectWrapper<>(data.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button renameButton = new Button("Renombrar");
            private final Button toggleButton = new Button();
            private final HBox box = new HBox(8, renameButton, toggleButton);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                renameButton.getStyleClass().add("table-action-button");
                toggleButton.getStyleClass().add("table-action-button");
                renameButton.setOnAction(e -> handleRename(getItem()));
                toggleButton.setOnAction(e -> handleToggleActive(getItem()));
            }

            @Override
            protected void updateItem(MovementType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setGraphic(null);
                    return;
                }
                toggleButton.setText(type.active() ? "Desactivar" : "Activar");
                toggleButton.setDisable(type.isSystemType() && type.active());
                setGraphic(box);
            }
        });

        table.setPlaceholder(EmptyStates.of(Icon.SETTINGS, "Aún no hay tipos registrados",
                "Los tipos que crees aparecerán aquí."));
    }

    private void handleRename(MovementType type) {
        if (type == null) {
            return;
        }
        MovementTypeFormDialog.showForRename(type).ifPresent(newName -> {
            try {
                movementTypeService.rename(type.id(), newName);
                refresh();
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al renombrar tipo {}", type.id(), e);
                Dialogs.error("Por ahora no fue posible renombrar el tipo. Intenta nuevamente.");
            }
        });
    }

    private void handleToggleActive(MovementType type) {
        if (type == null) {
            return;
        }
        boolean newActive = !type.active();
        String message = newActive
                ? "¿Activar el tipo \"" + type.name() + "\"? Volverá a estar disponible para registrar movimientos."
                : "¿Desactivar el tipo \"" + type.name() + "\"? Ya no podrá usarse para nuevos movimientos, pero se conservará el historial.";
        if (Dialogs.confirm("Confirmar", message, "Cancelar", newActive ? "Activar" : "Desactivar")) {
            try {
                movementTypeService.setActive(type.id(), newActive);
                refresh();
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al cambiar estado del tipo {}", type.id(), e);
                Dialogs.error("Por ahora no fue posible actualizar el tipo. Intenta nuevamente.");
            }
        }
    }

    @FXML
    private void handleNewIncomeType() {
        handleNewType(MovementKind.INCOME);
    }

    @FXML
    private void handleNewExpenseType() {
        handleNewType(MovementKind.EXPENSE);
    }

    private void handleNewType(MovementKind kind) {
        MovementTypeFormDialog.showForCreate(kind).ifPresent(name -> {
            try {
                movementTypeService.createCustomType(name, kind);
                refresh();
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al crear tipo", e);
                Dialogs.error("Por ahora no fue posible crear el tipo. Intenta nuevamente.");
            }
        });
    }

    private void refresh() {
        incomeTable.setItems(FXCollections.observableArrayList(movementTypeService.listByKind(MovementKind.INCOME, true)));
        expenseTable.setItems(FXCollections.observableArrayList(movementTypeService.listByKind(MovementKind.EXPENSE, true)));
    }
}
