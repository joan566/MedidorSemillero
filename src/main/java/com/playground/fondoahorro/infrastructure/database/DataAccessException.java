package com.playground.fondoahorro.infrastructure.database;

/**
 * Unchecked wrapper around SQLException so service/presentation code doesn't
 * have to handle checked exceptions from every repository call. The original
 * SQLException is preserved as the cause for logging.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
