package com.playground.fondoahorro.application.importing.dto;

import java.util.List;
import java.util.Optional;

public record ImportValidationResult(List<ImportError> errors, Optional<ImportPlan> plan) {

    public static ImportValidationResult failure(List<ImportError> errors) {
        return new ImportValidationResult(errors, Optional.empty());
    }

    public static ImportValidationResult success(ImportPlan plan) {
        return new ImportValidationResult(List.of(), Optional.of(plan));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
