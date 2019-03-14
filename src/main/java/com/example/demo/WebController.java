package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class WebController {
    @RequestMapping("/")
    public RedirectView index() {
        return new RedirectView("/login");
    }

    @RequestMapping("/login")
    public String login() {
        return "login.htm";
    }

    @RequestMapping("/dashboard")
    public String dashboard() {
        return "dashboard.htm";
    }
}
