create table if not exists local_file_system_settings
(
  ID SERIAL PRIMARY KEY,
  BACKUP_PATH VARCHAR(4096) not null,
  DATE TIMESTAMPTZ DEFAULT NOW()
);