package com.playground.fondoahorro.presentation.shared;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * A non-blocking notification for successful, non-sensitive confirmations
 * (e.g. "setting saved") — fades in, holds briefly, fades out on its own.
 * Sensitive confirmations (loans, payments, gifts, settlements, deactivating
 * a person, restoring a backup) stay as modal Dialogs — those need the
 * administrator to read and act, not a message that can be missed.
 */
public final class Toast {

    private Toast() {
    }

    public static void show(Window owner, String message) {
        Node icon = Icons.node(Icons.Icon.CHECK, 14, Color.WHITE);

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        HBox content = new HBox(10, icon, label);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-background-color: #166044; -fx-padding: 12 20; -fx-background-radius: 8;");

        Popup popup = new Popup();
        popup.getContent().add(content);
        popup.setOnShown(event -> {
            popup.setX(owner.getX() + owner.getWidth() - content.getWidth() - 32);
            popup.setY(owner.getY() + owner.getHeight() - 80);
        });
        popup.show(owner);

        content.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), content);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), content);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> popup.hide());

        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }
}
