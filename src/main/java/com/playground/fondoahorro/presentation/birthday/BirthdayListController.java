package com.playground.fondoahorro.presentation.birthday;

import com.playground.fondoahorro.application.birthday.BirthdayGiftService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.birthday.UpcomingBirthday;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.infrastructure.birthday.JdbcBirthdayGiftRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.settings.JdbcAppSettingsRepository;
import com.playground.fondoahorro.presentation.shared.Badges;
import com.playground.fondoahorro.presentation.shared.Badges.BadgeStyle;
import com.playground.fondoahorro.presentation.shared.DateFormatter;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import com.playground.fondoahorro.presentation.shared.EmptyStates;
import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import com.playground.fondoahorro.presentation.shared.MoneyFormatter;
import com.playground.fondoahorro.presentation.shared.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BirthdayListController {

    private static final Logger log = LoggerFactory.getLogger(BirthdayListController.class);

    @FXML
    private FlowPane cardsContainer;

    private final BirthdayGiftService birthdayGiftService = new BirthdayGiftService(
            new JdbcBirthdayGiftRepository(), new JdbcMovementRepository(), new JdbcMovementTypeRepository(),
            new JdbcPersonRepository(), new AppSettingsService(new JdbcAppSettingsRepository()));

    @FXML
    private void initialize() {
        refresh();
    }

    private void refresh() {
        cardsContainer.getChildren().clear();
        var upcoming = birthdayGiftService.listUpcoming();
        if (upcoming.isEmpty()) {
            cardsContainer.getChildren().add(EmptyStates.of(Icon.BIRTHDAYS, "Aún no hay personas activas registradas",
                    "Cuando actives personas con fecha de nacimiento aparecerán aquí."));
            return;
        }
        for (UpcomingBirthday item : upcoming) {
            cardsContainer.getChildren().add(buildCard(item));
        }
    }

    private VBox buildCard(UpcomingBirthday item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(230);

        Label name = new Label(item.person().name());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        name.setWrapText(true);

        Label date = new Label(DateFormatter.formatDayMonth(item.occurrenceDate()));
        date.getStyleClass().add("metric-label");

        Label age = new Label("Cumple " + item.turningAge() + " años");
        age.getStyleClass().add("metric-label");

        Label giftStatus = item.giftGiven()
                ? Badges.of("Regalo entregado", BadgeStyle.SUCCESS)
                : Badges.of("Regalo pendiente", BadgeStyle.WARNING);

        Money amountToShow = item.giftGiven() ? item.givenAmount() : birthdayGiftService.defaultGiftAmount();
        Label amountLabel = new Label(MoneyFormatter.format(amountToShow));
        amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(name, date, age, giftStatus, amountLabel);

        if (!item.giftGiven()) {
            Button registerButton = new Button("Registrar regalo");
            registerButton.getStyleClass().add("primary-button");
            registerButton.setMaxWidth(Double.MAX_VALUE);
            registerButton.setOnAction(e -> handleRegisterGift(item.person()));
            card.getChildren().add(registerButton);
        }

        return card;
    }

    private void handleRegisterGift(Person person) {
        Money defaultAmount = birthdayGiftService.defaultGiftAmount();
        BirthdayGiftFormDialog.show(person, defaultAmount).ifPresent(input -> {
            String message = "¿Registrar regalo de " + MoneyFormatter.format(Money.of(input.amount()))
                    + " para " + person.name() + "?";
            if (!Dialogs.confirm("Confirmar regalo", message, "Cancelar", "Registrar regalo")) {
                return;
            }
            try {
                birthdayGiftService.registerGift(person.id(), input.date(), Money.of(input.amount()), input.method(), input.notes());
                refresh();
                Toast.show(cardsContainer.getScene().getWindow(), "Regalo registrado.");
            } catch (IllegalArgumentException e) {
                Dialogs.error(e.getMessage());
            } catch (Exception e) {
                log.error("Error al registrar regalo para la persona {}", person.id(), e);
                Dialogs.error("Por ahora no fue posible registrar el regalo. Intenta nuevamente.");
            }
        });
    }
}
