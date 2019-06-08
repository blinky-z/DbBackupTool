package com.blog.controllers.RestApi;

import com.blog.settings.UserSettings;
import com.blog.webUI.renderModels.WebBackupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class RestApiClient {
    private static final Logger logger = LoggerFactory.getLogger(RestApiClient.class);

    private TestRestTemplate testRestTemplate;

    private UserSettings userSettings;

    private String jsessionId = null;

    private void login() {
        if (jsessionId == null) {
            String jsessionCookie;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", userSettings.getWebUILogin());
            body.add("password", userSettings.getWebUIPassword());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = testRestTemplate.exchange(
                    "/api/login", HttpMethod.POST, entity, String.class);

            jsessionCookie = Objects.requireNonNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

            jsessionId = jsessionCookie.substring(0, jsessionCookie.indexOf(";"));
        }
    }

    @Autowired
    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    void setTestRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
        login();

        testRestTemplate.getRestTemplate().getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                request.getHeaders().add("Cookie", jsessionId);
                logger.debug("Client request: {URL: {}, Method: {}, Headers: {}}", request.getURI(), request.getMethod(), request.getHeaders());
                return execution.execute(request, body);
            }
        });

        this.testRestTemplate = testRestTemplate;
    }

    List<WebBackupTask> getTaskStates() {
        logger.debug("Performing GET task states request...");

        WebBackupTask[] taskStateArray = testRestTemplate.getForObject("/api/get-states", WebBackupTask[].class);
        List<WebBackupTask> taskStateList = Arrays.asList(Objects.requireNonNull(taskStateArray));

        logger.debug("GET task states request is done. Task states: {}", taskStateList);

        return taskStateList;
    }
}
