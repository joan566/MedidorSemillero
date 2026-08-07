package com.playground.fondoahorro.presentation.person;

import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.presentation.shared.Dialogs;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Optional;

/** Modal create/edit form for a Person. The action button is blocked from closing the dialog while the form is invalid. */
public final class PersonFormDialog {

    private PersonFormDialog() {
    }

    public static Optional<PersonInput> showForCreate() {
        return show("Nueva persona", null, "Guardar persona");
    }

    public static Optional<PersonInput> showForEdit(Person person) {
        return show("Editar persona", person, "Guardar cambios");
    }

    private static Optional<PersonInput> show(String title, Person existing, String actionText) {
        try {
            FXMLLoader loader = new FXMLLoader(PersonFormDialog.class.getResource("/fxml/person/person_form.fxml"));
            Parent content = loader.load();
            PersonFormController controller = loader.getController();
            if (existing != null) {
                controller.setPerson(existing);
            }

            return Dialogs.showForm(title, content, actionText, controller::validate);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible abrir el formulario de persona.", e);
        }
    }
}
