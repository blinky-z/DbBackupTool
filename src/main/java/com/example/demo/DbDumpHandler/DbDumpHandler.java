package com.example.demo.DbDumpHandler;

import java.io.InputStream;

public interface DbDumpHandler {
    InputStream createDbDump();

    void restoreDbDump(InputStream dumpData);
}
