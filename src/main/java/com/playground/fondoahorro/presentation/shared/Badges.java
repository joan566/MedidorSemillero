package com.playground.fondoahorro.presentation.shared;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.function.Function;

/** Pill-shaped status labels — replaces plain colored text for "Estado" columns everywhere. */
public final class Badges {

    public enum BadgeStyle {
        SUCCESS("badge-success"),
        WARNING("badge-warning"),
        DANGER("badge-danger"),
        INFO("badge-info"),
        NEUTRAL("badge-neutral");

        private final String styleClass;

        BadgeStyle(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    private Badges() {
    }

    public static Label of(String text, BadgeStyle style) {
        Label label = new Label(text);
        label.getStyleClass().addAll("badge", style.styleClass);
        return label;
    }

    /** A TableColumn cell factory that renders the column's String value as a badge, styled by {@code styleSelector}. */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> cellFactory(Function<String, BadgeStyle> styleSelector) {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty || value == null ? null : Badges.of(value, styleSelector.apply(value)));
            }
        };
    }
}
