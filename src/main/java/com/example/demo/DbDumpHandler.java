package com.example.demo;

import models.Env;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public abstract class DbDumpHandler {
    Env env;

    protected InputStream dataStream;

    DbDumpHandler(Env env) {
        this.env = env;
    }

    public abstract void createDbDump() throws SQLException, IOException;

    public abstract void restoreDbDump(InputStream dumpData) throws IOException;

    // getDataStream - stream for reading raw database backup data
    public InputStream getDataStream() {
        return dataStream;
    }
}
