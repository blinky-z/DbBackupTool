create table if not exists database_settings
(
  ID       SERIAL PRIMARY KEY,
  TYPE     VARCHAR(64)               not null,
  HOST     VARCHAR(256)              not null,
  PORT     VARCHAR(16)               not null,
  NAME     VARCHAR(1024)             not null,
  LOGIN    VARCHAR(256)              not null,
  PASSWORD VARCHAR(256)              not null,
  DATE     TIMESTAMPTZ DEFAULT NOW() not null
);

create table if not exists storage_settings
(
  ID                SERIAL PRIMARY KEY,
  TYPE              VARCHAR(64)               not null,
  DATE              TIMESTAMPTZ DEFAULT NOW() not null,
  ADDITIONAL_FIELDS varchar(8192)             not null /*additional settings saved as json */
)