package com.sssupermarket.security;

import com.sssupermarket.entity.User;
import com.sssupermarket.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails details)) {
            throw new IllegalStateException("No authenticated user in context.");
        }
        User user = userService.findByEmail(details.getUsername());
        if (user == null) {
            throw new IllegalStateException("Authenticated user does not exist.");
        }
        return user;
    }

    public Long getId() {
        return get().getId();
    }
}
