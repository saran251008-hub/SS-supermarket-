package com.sssupermarket.controller;

import com.sssupermarket.service.CartService;
import com.sssupermarket.service.UserService;
import com.sssupermarket.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final CartService cartService;

    public GlobalModelAdvice(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api")) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails details) {
            User user = userService.findByEmail(details.getUsername());
            if (user != null) {
                model.addAttribute("currentUser", user);
                model.addAttribute("cartCount", cartService.getCartCount(user.getId()));
            }
        }
    }
}
