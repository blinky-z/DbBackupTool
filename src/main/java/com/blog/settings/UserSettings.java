package com.blog.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * This class contains required user config fields from <i>application.properties</i>.
 */
@Configuration
@ConfigurationProperties(prefix = "user-config")
public class UserSettings {
    /**
     * Web UI login
     */
    private String web_ui_Login;

    /**
     * Web UI password
     */
    private String web_ui_Password;

    public String getWebUILogin() {
        return web_ui_Login;
    }

    public void setWebUILogin(String web_ui_Login) {
        this.web_ui_Login = web_ui_Login;
    }

    public String getWebUIPassword() {
        return web_ui_Password;
    }

    public void setWebUIPassword(String web_ui_Password) {
        this.web_ui_Password = web_ui_Password;
    }
}
