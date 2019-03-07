package models;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import settings.DatabaseSettings;

public class Env {
    public JdbcTemplate jdbcTemplate;

    public Logger logger;

    public DatabaseSettings dbSettings;
}