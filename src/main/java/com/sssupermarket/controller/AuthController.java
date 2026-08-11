package com.sssupermarket.controller;

import com.sssupermarket.dto.AuthResponse;
import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.Role;
import com.sssupermarket.exception.InvalidRoleException;
import com.sssupermarket.exception.UserAlreadyExistsException;
import com.sssupermarket.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            AuthResponse response = authService.register(request, httpRequest, httpResponse);
            return "redirect:" + homeForRole(response.getRole());
        } catch (UserAlreadyExistsException | InvalidRoleException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("registerRequest", request);
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    private String homeForRole(Role role) {
        return switch (role) {
            case ADMIN -> "/admin";
            case SELLER -> "/seller";
            default -> "/products";
        };
    }
}
