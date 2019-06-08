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

    private final String time;

    private WebBackupTask(@NotNull Integer id, @NotNull String type, @NotNull String state, @NotNull String time) {
        this.id = id;
        this.type = type;
        this.state = state;
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

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "WebBackupTask{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", time='" + time + '\'' +
                '}';
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "with")
    public static final class Builder {
        private Integer id;
        private String type;
        private String state;
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

        public Builder withTime(String time) {
            this.time = time;
            return this;
        }

        public WebBackupTask build() {
            return new WebBackupTask(id, type, state, time);
        }
    }
}
