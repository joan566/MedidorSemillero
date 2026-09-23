package com.playground.fondoahorro.application.importing.service;

import com.playground.fondoahorro.application.importing.dto.ImportPlan;

public interface ImportService {

    record ImportSummary(int personsCreated, int savingsCreated, int loansCreated, int paymentsCreated) {
    }

    /** Expects a plan that came out of a successful ImportValidator.validate(...) — every cross-reference is assumed to resolve. */
    ImportSummary execute(ImportPlan plan);
}
