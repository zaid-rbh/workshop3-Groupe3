package com.devsecops.project.controller;

import com.devsecops.project.dto.RegisterForm;
import com.devsecops.project.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * VULNERABILITY #16: no validation of any field (empty username, empty password, weak password,
     * invalid e-mail address... everything is accepted).
     */
    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm form, Model model) {
        if (userService.usernameExists(form.getUsername())) {
            model.addAttribute("error", "This username is already taken.");
            return "register";
        }
        userService.register(form);
        return "redirect:/login?registered";
    }
}
