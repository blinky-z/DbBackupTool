package com.example.demo;

import com.example.demo.models.LoginRequest;
import com.example.demo.models.ResponseTransfer;
import com.example.demo.settings.UserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {
    @Autowired
    UserSettings userSettings;

    private static final String INVALID_Credentials = "Invalid credentials";

    private boolean checkCrendetials(LoginRequest loginRequest) {
        String login = loginRequest.getLogin();
        String password = loginRequest.getPassword();
        if (login == null || login.equals("") || password == null || password.equals("")) {
            return false;
        }

        return login.equals(userSettings.getLogin()) && password.equals(userSettings.getPassword());
    }

    @RequestMapping(value = "/api/login", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity login(@RequestBody LoginRequest loginRequest) {
        System.out.println("Got login request");

        if (!checkCrendetials(loginRequest)) {
            ResponseTransfer response = new ResponseTransfer();
            response.setBody(INVALID_Credentials);

            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity(HttpStatus.OK);
    }
}
