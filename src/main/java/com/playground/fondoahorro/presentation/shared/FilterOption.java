package com.playground.fondoahorro.presentation.shared;

/** A labeled option for a filter ComboBox; value is null for the "Todos/Todas" (no filter) choice. */
public record FilterOption<T>(String label, T value) {
    @Override
    public String toString() {
        return label;
    }
}
