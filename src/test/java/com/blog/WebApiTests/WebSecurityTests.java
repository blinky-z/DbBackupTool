package com.blog.WebApiTests;

import com.blog.ApplicationTests;
import com.blog.settings.UserSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSecurityTests extends ApplicationTests {
    private static String LOGIN_PAGE_URL;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap;
    @LocalServerPort
    private int port;
    @Autowired
    private UserSettings userSettings;

    @PostConstruct
    private void initializeLoginPageUrl() {
        LOGIN_PAGE_URL = "http://localhost:" + port + "/login";
    }

    @Test
    public void givenNonAuthRequestOnSecuredPath_shouldRespondWithError() {
        System.out.println(port);
        ResponseEntity<String> resp =
                restTemplate.exchange("/storage", HttpMethod.POST, null, String.class);
        assertEquals(LOGIN_PAGE_URL, Objects.requireNonNull(resp.getHeaders().getFirst("Location")));
    }

    @Test
    public void givenAuthRequestOnSecuredPath_shouldNotRespondWithError() {
        String settingsName = "givenAuthRequestOnSecuredPath_shouldNotRespondWithError";
        String jsessionId;
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", userSettings.getWebUILogin());
            body.add("password", userSettings.getWebUIPassword());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate
                    .exchange("/api/login", HttpMethod.POST, entity, String.class);
            jsessionId = Objects.requireNonNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
            jsessionId = jsessionId.substring(0, jsessionId.indexOf(";"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", jsessionId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(localFileSystemStorageSettingsAsMultiValueMap);
        body.add("settingsName", settingsName);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate
                .exchange("/storage", HttpMethod.POST, entity, String.class);
        System.out.println(resp.toString());
        assertNotEquals(LOGIN_PAGE_URL, Objects.requireNonNull(resp.getHeaders().getFirst("Location")));
    }

    @Test
    public void givenNonAuthRequestOnUnsecuredPath_shouldNotRespondWithError() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/css/dashboard.css", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
