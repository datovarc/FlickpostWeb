package co.flickpost.admin.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

@Controller
@RequestMapping("/")
public class HomeViewController {

    @GetMapping(value = "")
    @Transactional
    public String weightView() {
        return "redirect:/weight";
    }

}
