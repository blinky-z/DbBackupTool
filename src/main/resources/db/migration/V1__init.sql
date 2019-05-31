create table if not exists database_settings
(
    TYPE              VARCHAR(64)   not null,
    SETTINGS_NAME     varchar(256)  not null unique,
    HOST              VARCHAR(256)  not null,
    PORT              INTEGER       not null,
    NAME              VARCHAR(1024) not null,
    LOGIN             VARCHAR(256)  not null,
    PASSWORD          VARCHAR(256)  not null,
    ADDITIONAL_FIELDS varchar(8192) not null,
    DATE              TIMESTAMP     not null
);

create table if not exists storage_settings
(
    TYPE              VARCHAR(64)   not null,
    SETTINGS_NAME     varchar(256)  not null unique,
    DATE              TIMESTAMP     not null,
    ADDITIONAL_FIELDS varchar(8192) not null
);

create table if not exists backup_properties
(
    ID                    SERIAL PRIMARY KEY,
    DATE                  TIMESTAMP     not null,
    BACKUP_NAME           varchar(1024) not null,
    STORAGE_SETTINGS_NAME varchar(128)  not null,
    PROCESSORS            varchar(1024) not null
);

create table if not exists backup_tasks
(
    ID                   SERIAL PRIMARY KEY,
    DATE                 TIMESTAMP   not null,
    TYPE                 VARCHAR(64) not null,
    RUN_TYPE             VARCHAR(64) not null,
    STATE                VARCHAR(64) not null,
    BACKUP_PROPERTIES_ID int         not null
);

create table if not exists planned_backup_tasks
(
    ID                         SERIAL PRIMARY KEY,
    STATE                      varchar(64)   not null,
    DATABASE_SETTINGS_NAME     varchar(256)  not null,
    STORAGE_SETTINGS_NAME_LIST varchar(8096) not null,
    PROCESSORS                 varchar(1024) not null,
    LAST_STARTED_TIME          timestamp     not null,
    INTERVAL                   BIGINT        not null,
    EXECUTING_TASKS            VARCHAR(256)
);

create table if not exists error_tasks
(
    ID            SERIAL PRIMARY KEY,
    TASK_ID       INTEGER UNIQUE,
    ERROR_HANDLED BOOLEAN default false
);