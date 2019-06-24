package com.blog.webUI.renderModels;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents currently executing, completed or erroneous backup task.
 * <p>
 * Getters are required for thymeleaf expressions evaluating.
 */
@JsonDeserialize(builder = WebBackupTask.Builder.class)
public class WebBackupTask {
    private final Integer id;

    private final String type;

    private final String state;

    private final Boolean isError;

    private final Boolean isInterrupted;

    private final String time;

    private WebBackupTask(@NotNull Integer id, @NotNull String type, @NotNull String state, @NotNull Boolean isError,
                          @NotNull Boolean isInterrupted, @NotNull String time) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.isError = isError;
        this.isInterrupted = isInterrupted;
        this.time = time;
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

    public Boolean getError() {
        return isError;
    }

    public Boolean getInterrupted() {
        return isInterrupted;
    }

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "WebBackupTask{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", error=" + isError +
                ", isInterrupted=" + isInterrupted +
                ", time='" + time + '\'' +
                '}';
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    public static final class Builder {
        private Integer id;
        private String type;
        private Boolean isError;
        private String state;
        private Boolean isInterrupted;
        private String time;

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

        public Builder withIsInterrupted(Boolean isInterrupted) {
            this.isInterrupted = isInterrupted;
            return this;
        }

        public Builder withTime(String time) {
            this.time = time;
            return this;
        }

        public Builder withIsError(boolean isError) {
            this.isError = isError;
            return this;
        }

        public WebBackupTask build() {
            return new WebBackupTask(id, type, state, isError, isInterrupted, time);
        }
    }
}
