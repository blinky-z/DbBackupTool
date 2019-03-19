package com.example.demo.models.storage;

import java.util.Date;
import java.util.HashMap;

public interface StorageSettings {
    public int getId();

    public StorageType getStorageType();

    public HashMap<String, String> getProperties();

    public Date getDate();
}
