package com.example.demo;

import com.example.demo.models.StorageItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class WebController {
    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

    @RequestMapping("/login")
    public String login() {
        return "login.html";
    }

    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        List<StorageItem> storageList = new ArrayList<>();
        storageList.add(new StorageItem("dropbox0", "0", "storage sample0 from tempplate",
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));
        storageList.add(new StorageItem("dropbox1", "1", "storage sample1 from tempplate",
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));
        storageList.add(new StorageItem("dropbox1", "1", "storage sample1 from tempplate",
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));

        model.addAttribute("storageList", storageList);

        return "dashboard.html";
    }
}
