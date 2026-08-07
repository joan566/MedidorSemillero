package com.playground.fondoahorro.presentation.settlement;

import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.application.settlement.SettlementService;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.settlement.Settlement;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.savings.JdbcSavingRepository;
import com.playground.fondoahorro.infrastructure.settings.JdbcAppSettingsRepository;
import com.playground.fondoahorro.infrastructure.settlement.JdbcSettlementRepository;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.PercentageFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class SettlementDetailController {

    private static final Logger log = LoggerFactory.getLogger(SettlementDetailController.class);
    private static final DateTimeFormatter PREPARED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label personLabel;
    @FXML
    private Label yearLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label interestTitleLabel;
    @FXML
    private Label savingsLabel;
    @FXML
    private Label interestLabel;
    @FXML
    private Label totalLabel;

    private final SettlementService settlementService = new SettlementService(
            new JdbcSettlementRepository(), new JdbcSavingRepository(), new JdbcPersonRepository(),
            new AppSettingsService(new JdbcAppSettingsRepository()));
    private final PersonService personService = new PersonService(new JdbcPersonRepository());

    private long personId;
    private int year;
    private Runnable onBack;

    public void setContext(long personId, int year) {
        this.personId = personId;
        this.year = year;
        load();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    private void load() {
        Person person = personService.findById(personId).orElseThrow(() -> new IllegalStateException("La persona no existe."));
        Settlement calculated = settlementService.calculateForPerson(personId, year);

        personLabel.setText(person.name());
        yearLabel.setText(String.valueOf(year));
        interestTitleLabel.setText("INTERÉS (" + PercentageFormatter.format(calculated.interestRateBps()) + "%)");
        savingsLabel.setText(MoneyFormatter.format(calculated.savingsTotal()));
        interestLabel.setText(MoneyFormatter.format(calculated.interestAmount()));
        totalLabel.setText(MoneyFormatter.format(calculated.totalAmount()));

        settlementService.findPrepared(personId, year).ifPresentOrElse(
                prepared -> statusLabel.setText("Preparada el " + prepared.preparedAt().format(PREPARED_AT_FORMAT)),
                () -> statusLabel.setText("Pendiente de preparar"));
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void handlePrepare() {
        Person person = personService.findById(personId).orElseThrow();
        Settlement calculated = settlementService.calculateForPerson(personId, year);
        String message = "¿Preparar la liquidación de " + MoneyFormatter.format(calculated.totalAmount())
                + " para " + person.name() + " (año " + year + ")?";
        if (!Dialogs.confirm("Confirmar liquidación", message, "Cancelar", "Preparar liquidación")) {
            return;
        }
        try {
            settlementService.prepare(personId, year);
            load();
            Toast.show(personLabel.getScene().getWindow(), "Liquidación preparada.");
        } catch (IllegalArgumentException e) {
            Dialogs.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error al preparar liquidación de la persona {} para {}", personId, year, e);
            Dialogs.error("Por ahora no fue posible preparar la liquidación. Intenta nuevamente.");
        }
    }
}
