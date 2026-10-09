package com.sssupermarket.controller;

import com.sssupermarket.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final CurrentUser currentUser;

    public ProfileController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("user", currentUser.get());
        return "profile";
    }
}
