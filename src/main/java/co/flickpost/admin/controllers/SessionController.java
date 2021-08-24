package co.flickpost.admin.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SessionController {

    @GetMapping(value = "/login")
    public String loginView() {
        return "login";
    }

}
