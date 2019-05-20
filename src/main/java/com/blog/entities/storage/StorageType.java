package com.blog.entities.storage;

import java.util.Optional;

/**
 * Storage type.
 */
public enum StorageType {
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

    StorageType(String storageAsString) {
        this.storageAsString = storageAsString;
    }

    public static Optional<StorageType> of(String storage) {
        for (StorageType value : values()) {
            if (value.storageAsString.equals(storage)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}