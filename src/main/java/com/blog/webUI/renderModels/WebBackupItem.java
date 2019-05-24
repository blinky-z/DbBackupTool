package com.blog.webUI.renderModels;

/**
 * This class represents created backup information.
 * <p>
 * Getters are required for thymeleaf expressions evaluating.
 */
public class WebBackupItem {
    private final int id;

    private final String desc;

    private final String name;

    private final String time;

    private WebBackupItem(int id, String desc, String name, String time) {
        this.id = id;
        this.desc = desc;
        this.name = name;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "WebBackupItem{" +
                "id=" + id +
                ", desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                '}';
    }


    public static final class Builder {
        private int id;
        private String desc;
        private String name;
        private String time;

        public Builder() {
        }

        public Builder withId(int id) {
            this.id = id;
            return this;
        }

        public Builder withDesc(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withTime(String time) {
            this.time = time;
            return this;
        }

        public WebBackupItem build() {
            return new WebBackupItem(id, desc, name, time);
        }
    }
}
