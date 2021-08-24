package co.flickpost.admin.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelControllerAdvice {

    @ModelAttribute
    public void addAttributes(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
    }

}
