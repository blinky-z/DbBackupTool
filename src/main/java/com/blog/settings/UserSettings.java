package com.blog.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "user-config")
public class UserSettings {
    private String webUILogin;

    private String webUIPassword;

    public String getWebUILogin() {
        return webUILogin;
    }

    public void setWebUILogin(String webUILogin) {
        this.webUILogin = webUILogin;
    }

    public String getWebUIPassword() {
        return webUIPassword;
    }

    public void setWebUIPassword(String webUIPassword) {
        this.webUIPassword = webUIPassword;
    }
}
