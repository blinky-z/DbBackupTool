create table if not exists dropbox_settings
(
  ID SERIAL PRIMARY KEY,
  ACCESS_TOKEN VARCHAR(256) not null,
  DATE TIMESTAMPTZ DEFAULT NOW()
);