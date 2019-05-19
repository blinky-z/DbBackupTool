package com.blog.webUI.renderModels;

import com.blog.entities.database.DatabaseType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This class represents database settings
 */
public class WebDatabaseItem {
    private final String settingsName;

    private final DatabaseType type;

    private final String desc;

    private final String time;

    private WebDatabaseItem(@NotNull DatabaseType type, @NotNull String settingsName, @NotNull String desc, @NotNull String time) {
        this.type = Objects.requireNonNull(type);
        this.settingsName = Objects.requireNonNull(settingsName);
        this.desc = Objects.requireNonNull(desc);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public String toString() {
        return "WebDatabaseItem{" +
                "settingsName='" + settingsName + '\'' +
                ", type=" + type +
                ", desc='" + desc + '\'' +
                ", time='" + time + '\'' +
                '}';
    }


    public static final class Builder {
        private String settingsName;
        private DatabaseType type;
        private String desc;
        private String time;

        public Builder() {
        }

        public Builder withSettingsName(String settingsName) {
            this.settingsName = settingsName;
            return this;
        }

        public Builder withType(DatabaseType type) {
            this.type = type;
            return this;
        }

        public Builder withDesc(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder withTime(String time) {
            this.time = time;
            return this;
        }

        public WebDatabaseItem build() {
            return new WebDatabaseItem(type, settingsName, desc, time);
        }
    }
}
