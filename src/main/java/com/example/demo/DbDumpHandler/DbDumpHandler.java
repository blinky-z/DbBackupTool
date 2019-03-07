package com.example.demo.DbDumpHandler;

import models.Env;

import java.io.InputStream;

public abstract class DbDumpHandler {
    Env env;

    InputStream dataStream;

    DbDumpHandler(Env env) {
        this.env = env;
    }

    public abstract void createDbDump();

    public abstract void restoreDbDump(InputStream dumpData);

    // getDataStream - stream for reading raw database backup data
    public InputStream getDataStream() {
        return dataStream;
    }
}
