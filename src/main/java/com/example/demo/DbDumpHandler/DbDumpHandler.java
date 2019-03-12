package com.example.demo.DbDumpHandler;

import java.io.InputStream;

/**
 * This interface provides API to work with backups of any database.
 */
public interface DbDumpHandler {
    /**
     * Creates database backup.
     * @return input stream contains the plain text database backup
     */
    InputStream createDbDump();

    /**
     * Restores database backup.
     * @param dumpData input stream contains the plain text database backup
     */
    void restoreDbDump(InputStream dumpData);
}
