package com.blog.webUI.renderModels;

import com.blog.entities.storage.StorageType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This class represents storage settings
 */
public class WebStorageItem {
    private final String settingsName;

    private final StorageType type;

    private final String desc;

    private final String time;

    private WebStorageItem(@NotNull StorageType type, @NotNull String settingsName, @NotNull String desc, @NotNull String time) {
        this.type = Objects.requireNonNull(type);
        this.settingsName = Objects.requireNonNull(settingsName);
        this.desc = Objects.requireNonNull(desc);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    public String toString() {
        return "WebStorageItem{" +
                "settingsName='" + settingsName + '\'' +
                ", type=" + type +
                ", desc='" + desc + '\'' +
                ", time='" + time + '\'' +
                '}';
    }


    public static final class Builder {
        private String settingsName;
        private StorageType type;
        private String desc;
        private String time;

        public Builder() {
        }

        public Builder withSettingsName(String settingsName) {
            this.settingsName = settingsName;
            return this;
        }

        public Builder withType(StorageType type) {
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

        public WebStorageItem build() {
            return new WebStorageItem(type, settingsName, desc, time);
        }
    }
}
