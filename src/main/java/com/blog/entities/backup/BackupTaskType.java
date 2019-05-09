package com.blog.entities.backup;

public enum BackupTaskType {
    CREATE_BACKUP {
        @Override
        public String toString() {
            return "CREATE BACKUP";
        }
    },
    RESTORE_BACKUP {
        @Override
        public String toString() {
            return "RESTORE BACKUP";
        }
    }
}
