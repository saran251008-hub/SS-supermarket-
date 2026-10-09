package com.sssupermarket.controller;

import com.sssupermarket.dto.ApiResponse;
import com.sssupermarket.dto.AuthResponse;
import com.sssupermarket.dto.LoginRequest;
import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.dto.OtpRequest;
import com.sssupermarket.dto.OtpVerifyRequest;
import com.sssupermarket.entity.User;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.AuthService;
import com.sssupermarket.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final OtpService otpService;

    public AuthApiController(AuthService authService, CurrentUser currentUser, OtpService otpService) {
        this.authService = authService;
        this.currentUser = currentUser;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        AuthResponse response = authService.register(request, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<OtpService.OtpResult> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(otpService.request(request));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        return ResponseEntity.ok(otpService.verify(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(new ApiResponse("Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        User user = currentUser.get();
        AuthResponse response = new AuthResponse(user.getId(), user.getName(), user.getEmail(),
            user.getRole(), "Authenticated.");
        response.setPhone(user.getPhone());
        return ResponseEntity.ok(response);
    }
}
