package com.blog.entities.backup;

public enum BackupTaskState {
    PLANNED,
    CREATING,
    RESTORING,
    DELETING,
    APPLYING_PROCESSORS,
    APPLYING_DEPROCESSORS,
    DOWNLOADING,
    UPLOADING,
    COMPLETED
}
