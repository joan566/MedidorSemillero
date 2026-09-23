package com.playground.fondoahorro.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A fund participant. Not a system user — the application has no login,
 * roles, or accounts; a Person is simply a record the administrator manages.
 */
public record Person(
        Long id,
        String name,
        LocalDate birthDate,
        String phone,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public Person {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ingresa el nombre de la persona.");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("Selecciona la fecha de nacimiento.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        name = name.trim();
        phone = (phone == null || phone.isBlank()) ? null : phone.trim();
    }

    public static Person newPerson(String name, LocalDate birthDate, String phone) {
        return new Person(null, name, birthDate, phone, true, null, null);
    }

    public Person withUpdatedDetails(String name, LocalDate birthDate, String phone) {
        return new Person(id, name, birthDate, phone, active, createdAt, updatedAt);
    }

    public Person withActive(boolean active) {
        return new Person(id, name, birthDate, phone, active, createdAt, updatedAt);
    }
}
