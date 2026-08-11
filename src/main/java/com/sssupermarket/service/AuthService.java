package com.sssupermarket.service;

import com.sssupermarket.dto.AuthResponse;
import com.sssupermarket.dto.LoginRequest;
import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.InvalidCredentialsException;
import com.sssupermarket.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(AuthenticationManager authenticationManager,
                       UserService userService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        User user = userService.register(request);
        authenticateAndSetSession(httpRequest, httpResponse, user.getEmail(), request.getPassword());
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                "Registration successful. You are now logged in.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        String email = request.getEmail().trim().toLowerCase();
        authenticateAndSetSession(httpRequest, httpResponse, email, request.getPassword());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                "Login successful.");
    }

    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void authenticateAndSetSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                           String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }
    }
}
