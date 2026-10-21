package com.devsecops.project.controller;

import com.devsecops.project.model.User;
import com.devsecops.project.service.CryptoService;
import com.devsecops.project.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class ProfileController {

    private final UserService userService;
    private final CryptoService cryptoService;

    public ProfileController(UserService userService, CryptoService cryptoService) {
        this.userService = userService;
        this.cryptoService = cryptoService;
    }

    @GetMapping("/profile")
    public String myProfile(Principal principal) {
        User user = userService.getByUsername(principal.getName());
        return "redirect:/profile/" + user.getId();
    }

    /**
     * VULNERABILITY #9: insecure direct object reference (IDOR).
     * Nothing checks that the profile id belongs to the logged-in user, so /profile/1, /profile/2...
     * show other people's data (including their decrypted fictional card number).
     */
    @GetMapping("/profile/{id}")
    public String profile(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("profileUser", user);
        model.addAttribute("cardNumber", cryptoService.decrypt(user.getCardNumber()));
        return "profile";
    }

    /**
     * VULNERABILITY #9 (write side): any logged-in user can modify any profile.
     * VULNERABILITY #12: mass assignment. The whole User object is bound from the request, and the
     * "role" parameter is copied too, so a user can promote himself to ADMIN.
     */
    @PostMapping("/profile/{id}")
    public String update(@PathVariable Long id, @ModelAttribute User form,
                         RedirectAttributes redirectAttributes) {
        User existing = userService.getById(id);
        existing.setFullName(form.getFullName());
        existing.setEmail(form.getEmail());
        existing.setAddress(form.getAddress());
        if (form.getCardNumber() != null && !form.getCardNumber().isBlank()) {
            existing.setCardNumber(cryptoService.encrypt(form.getCardNumber()));
        }
        if (form.getRole() != null && !form.getRole().isBlank()) {
            existing.setRole(form.getRole());
        }
        userService.save(existing);
        redirectAttributes.addFlashAttribute("message", "Profile updated.");
        return "redirect:/profile/" + id;
    }
}
