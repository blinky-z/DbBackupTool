package com.blog.webUI.renderModels;


import org.jetbrains.annotations.NotNull;

/**
 * This class represents currently executing, completed or erroneous backup task.
 * <p>
 * Getters are required for thymeleaf expressions evaluating.
 */
public class WebBackupTask {
    private final Integer id;

    private final String type;

    private final String state;

    private final String time;

    private final Boolean isError;

    private WebBackupTask(@NotNull Integer id, @NotNull String type, @NotNull String state,
                          @NotNull String time, @NotNull Boolean isError) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.time = time;
        this.isError = isError;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public String getTime() {
        return time;
    }

    public Boolean getError() {
        return isError;
    }

    @Override
    public String toString() {
        return "WebBackupTask{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", time='" + time + '\'' +
                ", isError=" + isError +
                '}';
    }


    public static final class Builder {
        private Integer id;
        private String type;
        private String state;
        private String time;
        private Boolean isError;

        public Builder() {
        }

        public Builder withId(Integer id) {
            this.id = id;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withState(String state) {
            this.state = state;
            return this;
        }

        public Builder withTime(String time) {
            this.time = time;
            return this;
        }

        public Builder withIsError(Boolean isError) {
            this.isError = isError;
            return this;
        }

        public WebBackupTask build() {
            return new WebBackupTask(id, type, state, time, isError);
        }
    }
}
