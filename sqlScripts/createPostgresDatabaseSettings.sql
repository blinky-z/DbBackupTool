create table if not exists postgres_settings
(
  ID       SERIAL PRIMARY KEY,
  HOST     VARCHAR(256)  not null,
  PORT     VARCHAR(16)   not null,
  NAME     VARCHAR(1024) not null,
  LOGIN    VARCHAR(256)  not null,
  PASSWORD VARCHAR(256)  not null,
  DATE     TIMESTAMPTZ DEFAULT NOW()
);