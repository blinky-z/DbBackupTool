package com.blog.configuration;

import com.blog.ApplicationTests;
import com.blog.settings.UserSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSecurityTests extends ApplicationTests {
    private String LOGIN_PAGE_URL;
    @Autowired
    private TestRestTemplate restTemplate;
    @LocalServerPort
    private int port;
    @Autowired
    private UserSettings userSettings;

    @PostConstruct
    private void initializeLoginPageUrl() {
        LOGIN_PAGE_URL = "http://localhost:" + port + "/login";
    }

    @Test
    void givenNonAuthRequestOnSecuredPath_shouldRespondWithError() {
        ResponseEntity<String> resp =
                restTemplate.exchange("/storage", HttpMethod.POST, null, String.class);
        assertEquals(LOGIN_PAGE_URL, Objects.requireNonNull(resp.getHeaders().getFirst("Location")));
    }

    @Test
    void givenAuthRequestOnSecuredPath_shouldNotRespondWithError() {
        String jsessionId;
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", userSettings.getWebUILogin());
            body.add("password", userSettings.getWebUIPassword());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    "/api/login", HttpMethod.POST, entity, String.class);

            String jsessionCookie = Objects.requireNonNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
            jsessionId = jsessionCookie.substring(0, jsessionCookie.indexOf(";"));
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Cookie", jsessionId);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(httpHeaders);

        ResponseEntity<String> resp = restTemplate.exchange("/dashboard", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void givenNonAuthRequestOnUnsecuredPath_shouldNotRespondWithError() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/css/dashboard.css", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
