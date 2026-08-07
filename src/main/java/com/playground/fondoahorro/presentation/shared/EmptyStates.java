package com.playground.fondoahorro.presentation.shared;

import com.playground.fondoahorro.presentation.shared.Icons.Icon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * A friendly, on-brand placeholder for empty tables/lists — replaces bare
 * "no data" text everywhere (section 22 of the redesign brief). Used via
 * {@code table.setPlaceholder(EmptyStates.of(...))}.
 */
public final class EmptyStates {

    private static final Color ICON_COLOR = Color.web("#D1D5DB");

    private EmptyStates() {
    }

    public static Node of(Icon icon, String title, String subtitle) {
        return of(icon, title, subtitle, null, null);
    }

    public static Node of(Icon icon, String title, String subtitle, String buttonText, Runnable onAction) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));

        Node iconNode = Icons.node(icon, 40, ICON_COLOR);
        VBox.setMargin(iconNode, new Insets(0, 0, 8, 0));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("empty-state-subtitle");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(iconNode, titleLabel, subtitleLabel);

        if (buttonText != null && onAction != null) {
            Button button = new Button(buttonText);
            button.getStyleClass().add("primary-button");
            button.setOnAction(e -> onAction.run());
            VBox.setMargin(button, new Insets(10, 0, 0, 0));
            box.getChildren().add(button);
        }

        return box;
    }
}
