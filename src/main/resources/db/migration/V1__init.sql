create table if not exists database_settings
(
    TYPE              VARCHAR(64)               not null,
    SETTINGS_NAME     varchar(256)              not null unique,
    HOST              VARCHAR(256)              not null,
    PORT              INTEGER                   not null,
    NAME              VARCHAR(1024)             not null,
    LOGIN             VARCHAR(256)              not null,
    PASSWORD          VARCHAR(256)              not null,
    ADDITIONAL_FIELDS varchar(8192)             not null,
    DATE              TIMESTAMPTZ DEFAULT NOW() not null
);

create table if not exists storage_settings
(
    TYPE              VARCHAR(64)               not null,
    SETTINGS_NAME     varchar(256)              not null unique,
    DATE              TIMESTAMPTZ DEFAULT NOW() not null,
    ADDITIONAL_FIELDS varchar(8192)             not null
);

create table if not exists backup_properties
(
    ID                    SERIAL PRIMARY KEY,
    DATE                  TIMESTAMPTZ DEFAULT NOW() not null,
    BACKUP_NAME           varchar(1024)             not null,
    STORAGE_SETTINGS_NAME varchar(128)              not null,
    PROCESSORS            varchar(1024)             not null
);