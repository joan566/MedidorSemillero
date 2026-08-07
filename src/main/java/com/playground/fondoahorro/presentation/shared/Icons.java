package com.playground.fondoahorro.presentation.shared;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * A small consistent icon set, built from plain JavaFX shapes (no SVG data,
 * no font files, no external assets) so the app stays fully offline and
 * dependency-free. Every icon is drawn in a size×size box from coordinates
 * expressed as fractions of size, so they stay crisp at any requested size.
 */
public final class Icons {

    public enum Icon {
        DASHBOARD, PERSONS, SAVINGS, LOANS, BIRTHDAYS, SETTLEMENTS, MOVEMENTS, SETTINGS,
        ARROW_UP, ARROW_DOWN, SEARCH, CHECK
    }

    private Icons() {
    }

    /** Color driven entirely by the ".icon" CSS class (and its :hover/:selected descendant rules) — for the sidebar. */
    public static Node node(Icon icon, double size) {
        Group group = build(icon, size);
        for (javafx.scene.Node child : group.getChildren()) {
            if (child instanceof Shape shape) {
                shape.getStyleClass().add("icon");
            }
        }
        return group;
    }

    /** Explicit color, set directly — for every other context (cards, tables, toasts, empty states). */
    public static Node node(Icon icon, double size, Paint color) {
        Group group = build(icon, size);
        for (javafx.scene.Node child : group.getChildren()) {
            if (child instanceof Circle circle && circle.getFill() == Color.TRANSPARENT) {
                circle.setStroke(color);
            } else if (child instanceof Shape shape) {
                shape.setFill(color);
            }
        }
        return group;
    }

    private static Group build(Icon icon, double s) {
        return switch (icon) {
            case DASHBOARD -> dashboard(s);
            case PERSONS -> persons(s);
            case SAVINGS -> savings(s);
            case LOANS -> loans(s);
            case BIRTHDAYS -> birthdays(s);
            case SETTLEMENTS -> settlements(s);
            case MOVEMENTS -> movements(s);
            case SETTINGS -> settings(s);
            case ARROW_UP -> arrow(s, true);
            case ARROW_DOWN -> arrow(s, false);
            case SEARCH -> search(s);
            case CHECK -> check(s);
        };
    }

    private static Group dashboard(double s) {
        double gap = s * 0.12;
        double cell = (s - gap) / 2;
        Group g = new Group();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                Rectangle r = new Rectangle(col * (cell + gap), row * (cell + gap), cell, cell);
                r.setArcWidth(cell * 0.35);
                r.setArcHeight(cell * 0.35);
                g.getChildren().add(r);
            }
        }
        return g;
    }

    private static Group persons(double s) {
        Circle head = new Circle(s * 0.5, s * 0.30, s * 0.18);
        Polygon body = new Polygon(
                s * 0.22, s * 0.92,
                s * 0.30, s * 0.58,
                s * 0.70, s * 0.58,
                s * 0.78, s * 0.92);
        Group g = new Group(head, body);
        return g;
    }

    private static Group savings(double s) {
        Group g = new Group();
        double barW = s * 0.18;
        double gap = s * 0.10;
        double baseline = s * 0.85;
        double[] heights = {s * 0.35, s * 0.55, s * 0.75};
        for (int i = 0; i < 3; i++) {
            double h = heights[i];
            double x = s * 0.12 + i * (barW + gap);
            Rectangle bar = new Rectangle(x, baseline - h, barW, h);
            bar.setArcWidth(barW * 0.4);
            bar.setArcHeight(barW * 0.4);
            g.getChildren().add(bar);
        }
        return g;
    }

    private static Group loans(double s) {
        double shaftH = s * 0.10;
        Rectangle topShaft = new Rectangle(s * 0.15, s * 0.30, s * 0.55, shaftH);
        Polygon topHead = new Polygon(
                s * 0.70, s * 0.22,
                s * 0.70, s * 0.48,
                s * 0.88, s * 0.35);
        Rectangle bottomShaft = new Rectangle(s * 0.30, s * 0.60, s * 0.55, shaftH);
        Polygon bottomHead = new Polygon(
                s * 0.30, s * 0.52,
                s * 0.30, s * 0.78,
                s * 0.12, s * 0.65);
        return new Group(topShaft, topHead, bottomShaft, bottomHead);
    }

    private static Group birthdays(double s) {
        Rectangle box = new Rectangle(s * 0.18, s * 0.40, s * 0.64, s * 0.48);
        box.setArcWidth(s * 0.08);
        box.setArcHeight(s * 0.08);
        Rectangle lid = new Rectangle(s * 0.14, s * 0.30, s * 0.72, s * 0.14);
        lid.setArcWidth(s * 0.06);
        lid.setArcHeight(s * 0.06);
        Polygon bow = new Polygon(
                s * 0.42, s * 0.30,
                s * 0.58, s * 0.30,
                s * 0.50, s * 0.16);
        return new Group(box, lid, bow);
    }

    private static Group settlements(double s) {
        double t = s * 0.08;
        double x = s * 0.18;
        double y = s * 0.10;
        double w = s * 0.64;
        double h = s * 0.80;
        Group g = new Group(
                new Rectangle(x, y, w, t),
                new Rectangle(x, y + h - t, w, t),
                new Rectangle(x, y, t, h),
                new Rectangle(x + w - t, y, t, h));
        double lineH = s * 0.07;
        double lineX = x + s * 0.10;
        double[] lineWidths = {w * 0.6, w * 0.6, w * 0.4};
        double[] lineYs = {y + h * 0.30, y + h * 0.50, y + h * 0.70};
        for (int i = 0; i < 3; i++) {
            g.getChildren().add(new Rectangle(lineX, lineYs[i], lineWidths[i], lineH));
        }
        return g;
    }

    private static Group movements(double s) {
        Group g = new Group();
        double h = s * 0.12;
        double gap = s * 0.14;
        double[] widths = {s * 0.76, s * 0.60, s * 0.44};
        for (int i = 0; i < 3; i++) {
            Rectangle bar = new Rectangle(s * 0.12, s * 0.20 + i * (h + gap), widths[i], h);
            bar.setArcWidth(h * 0.6);
            bar.setArcHeight(h * 0.6);
            g.getChildren().add(bar);
        }
        return g;
    }

    private static Group settings(double s) {
        Group g = new Group();
        double trackH = s * 0.06;
        double trackW = s * 0.70;
        double trackX = s * 0.15;
        double[] trackYs = {s * 0.22, s * 0.50, s * 0.78};
        double[] knobOffsets = {0.65, 0.30, 0.50};
        double knobR = s * 0.09;
        for (int i = 0; i < 3; i++) {
            Rectangle track = new Rectangle(trackX, trackYs[i] - trackH / 2, trackW, trackH);
            track.setArcWidth(trackH);
            track.setArcHeight(trackH);
            Circle knob = new Circle(trackX + trackW * knobOffsets[i], trackYs[i], knobR);
            g.getChildren().addAll(track, knob);
        }
        return g;
    }

    private static Group arrow(double s, boolean up) {
        double shaftW = s * 0.16;
        double shaftH = s * 0.45;
        double shaftX = (s - shaftW) / 2;
        double shaftY = up ? s * 0.45 : s * 0.10;
        Rectangle shaft = new Rectangle(shaftX, shaftY, shaftW, shaftH);
        Polygon head = up
                ? new Polygon(s * 0.5, s * 0.08, s * 0.22, s * 0.42, s * 0.78, s * 0.42)
                : new Polygon(s * 0.5, s * 0.92, s * 0.22, s * 0.58, s * 0.78, s * 0.58);
        return new Group(shaft, head);
    }

    private static Group search(double s) {
        double r = s * 0.30;
        double cx = s * 0.40;
        double cy = s * 0.40;
        Circle ring = new Circle(cx, cy, r);
        ring.setFill(Color.TRANSPARENT);
        ring.setStrokeWidth(s * 0.09);
        Rectangle handle = new Rectangle(cx + r * 0.72, cy + r * 0.72, s * 0.34, s * 0.10);
        handle.setRotate(45);
        return new Group(ring, handle);
    }

    private static Group check(double s) {
        Polygon mark = new Polygon(
                s * 0.18, s * 0.52,
                s * 0.40, s * 0.74,
                s * 0.84, s * 0.26,
                s * 0.76, s * 0.18,
                s * 0.40, s * 0.56,
                s * 0.26, s * 0.42);
        return new Group(mark);
    }
}
