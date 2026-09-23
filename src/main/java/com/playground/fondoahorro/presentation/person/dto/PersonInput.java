package com.playground.fondoahorro.presentation.person.dto;

import java.time.LocalDate;

/** Values collected from the person create/edit form, before they reach the service layer. */
public record PersonInput(String name, LocalDate birthDate, String phone) {
}
