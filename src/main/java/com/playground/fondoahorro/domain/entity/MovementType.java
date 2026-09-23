package com.playground.fondoahorro.domain.entity;

import java.time.LocalDateTime;
import com.playground.fondoahorro.domain.enums.MovementKind;

/**
 * A configurable income/expense category (e.g. "Ahorro", "Otro egreso").
 * Types with a non-null code are relied on by the app's automatic flows
 * (savings, loans, loan payments, birthday gifts, settlements) to record
 * their movements, and can only be renamed — never deactivated — so those
 * flows always have a category to write to. Types without a code are
 * fully managed by the administrator: create, rename, activate/deactivate.
 */
public record MovementType(
        Long id,
        String name,
        MovementKind kind,
        String code,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public MovementType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ingresa el nombre del tipo.");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Selecciona si el tipo es de ingreso o egreso.");
        }
        name = name.trim();
    }

    public static MovementType newCustomType(String name, MovementKind kind) {
        return new MovementType(null, name, kind, null, true, null, null);
    }

    public boolean isSystemType() {
        return code != null;
    }

    public MovementType withName(String name) {
        return new MovementType(id, name, kind, code, active, createdAt, updatedAt);
    }

    public MovementType withActive(boolean active) {
        return new MovementType(id, name, kind, code, active, createdAt, updatedAt);
    }
}
