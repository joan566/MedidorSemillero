package com.playground.fondoahorro.infrastructure.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;

/**
 * Plain file copy of the SQLite database file — safe here because the app
 * never keeps a connection open between operations (see DatabaseManager),
 * so there is never a write in flight to copy mid-transaction.
 */
public final class DatabaseBackup {

    private static final String SQLITE_MAGIC = "SQLite format 3";

    private DatabaseBackup() {
    }

    public static void copyTo(Path destination) throws IOException {
        Files.copy(DatabaseManager.getDatabaseFilePath(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Replaces the live database file with the given backup. The caller is
     * responsible for closing the application afterward — connections opened
     * before the restore may otherwise reference stale file state.
     */
    public static void restoreFrom(Path source) throws IOException {
        if (!looksLikeSqliteFile(source)) {
            throw new IOException("El archivo seleccionado no parece ser una copia de seguridad válida.");
        }
        Files.copy(source, DatabaseManager.getDatabaseFilePath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean looksLikeSqliteFile(Path path) throws IOException {
        byte[] header = new byte[16];
        try (InputStream in = Files.newInputStream(path)) {
            if (in.read(header) < 16) {
                return false;
            }
        }
        return new String(header, StandardCharsets.US_ASCII).startsWith(SQLITE_MAGIC);
    }
}
