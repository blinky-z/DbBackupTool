package com.example.demo.entities.storage;

import java.util.Optional;

public enum Storage {
    DROPBOX("dropbox") {
        public String toString() {
            return "Dropbox";
        }
    },
    LOCAL_FILE_SYSTEM("localFileSystem") {
        public String toString() {
            return "Local File System";
        }
    };

    private final String storageAsString;

    Storage(String storageAsString) {
        this.storageAsString = storageAsString;
    }

    public static Optional<Storage> of(String storage) {
        for (Storage value : values()) {
            if (value.storageAsString.equals(storage)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}