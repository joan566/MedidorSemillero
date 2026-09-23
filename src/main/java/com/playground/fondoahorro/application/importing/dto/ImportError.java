package com.playground.fondoahorro.application.importing.dto;

/** One thing wrong with the workbook — which sheet, which Excel row, and why. */
public record ImportError(String sheet, int row, String message) {
}
