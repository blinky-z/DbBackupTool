package com.example.demo.models.storage;

public enum StorageType {
    DROPBOX {
        public String toString() {
            return "Dropbox";
        }
    }, LOCAL_FILE_SYSTEM {
        public String toString() {
            return "Local File System";
        }
    }
}