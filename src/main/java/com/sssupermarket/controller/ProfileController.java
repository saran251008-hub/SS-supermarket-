package com.sssupermarket.controller;

import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ProfileController {

    private final CurrentUser currentUser;
    private final UserService userService;

    public ProfileController(CurrentUser currentUser, UserService userService) {
        this.currentUser = currentUser;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("user", currentUser.get());
        return "profile";
    }

    @PostMapping("/profile/upgrade-seller")
    public String upgradeToSeller(@RequestParam(required = false) String phone,
                                  RedirectAttributes redirectAttributes) {
        User user = currentUser.get();
        if (user.getRole() == Role.ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Admin accounts already have all privileges.");
            return "redirect:/profile";
        }

        String contactPhone = (phone != null && !phone.isBlank()) ? phone.trim() : user.getPhone();
        if (contactPhone == null || !contactPhone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("errorMessage", "A valid 10-digit mobile number is required to register as a seller.");
            return "redirect:/profile";
        }

        userService.upgradeToSeller(user.getId(), contactPhone);

        // Update active authentication in SecurityContext
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            List<GrantedAuthority> updatedAuthorities = new ArrayList<>(currentAuth.getAuthorities());
            updatedAuthorities.removeIf(a -> a.getAuthority().startsWith("ROLE_"));
            updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    currentAuth.getPrincipal(),
                    currentAuth.getCredentials(),
                    updatedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Congratulations! Your account has been upgraded to Seller. You can now list and manage your products.");
        return "redirect:/seller";
    }
}
