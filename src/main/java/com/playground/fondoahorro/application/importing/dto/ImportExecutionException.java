package com.playground.fondoahorro.application.importing.dto;

/**
 * A row passed ImportValidator's checks but was rejected by the domain logic
 * itself once actually executed — e.g. a payment that (after replaying every
 * prior operation) turns out to exceed what's owed, or a loan the savings
 * fund can't actually cover at that point in its history. Rare, since
 * ImportValidator already catches the common data-entry mistakes, but always
 * possible because those two rules depend on the full chronological replay,
 * not just the row in isolation.
 */
public class ImportExecutionException extends RuntimeException {

    private final String sheet;
    private final int row;

    public ImportExecutionException(String sheet, int row, String message) {
        super(message);
        this.sheet = sheet;
        this.row = row;
    }

    public String sheet() {
        return sheet;
    }

    public int row() {
        return row;
    }
}
