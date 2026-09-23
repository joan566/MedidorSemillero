package com.playground.fondoahorro.presentation.birthday.controller;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;
import com.playground.fondoahorro.presentation.birthday.dto.BirthdayGiftInput;

public final class BirthdayGiftFormDialog {

    private BirthdayGiftFormDialog() {
    }

    public static Optional<BirthdayGiftInput> show(Person person, Money defaultAmount) {
        try {
            FXMLLoader loader = new FXMLLoader(BirthdayGiftFormDialog.class.getResource("/fxml/birthday/birthday_gift_form.fxml"));
            Parent content = loader.load();
            BirthdayGiftFormController controller = loader.getController();
            controller.setContext(person, defaultAmount);

            return Dialogs.showForm("Registrar regalo de cumpleaños", content, "Registrar regalo", controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de regalo.", e);
        }
    }
}
