package com.sssupermarket.service;

import com.sssupermarket.dto.AuthResponse;
import com.sssupermarket.dto.OtpRequest;
import com.sssupermarket.dto.OtpVerifyRequest;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.InvalidCredentialsException;
import com.sssupermarket.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final String OTP_SESSION_KEY = "otpChallenge";
    private static final long OTP_TTL_SECONDS = 300;
    private static final long RESEND_DELAY_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeDevelopmentCode;
    private final String mailHost;
    private final JavaMailSender mailSender;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public OtpService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.auth.otp.expose-development-code:false}") boolean exposeDevelopmentCode,
                      @Value("${spring.mail.host:}") String mailHost,
                      ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.exposeDevelopmentCode = exposeDevelopmentCode;
        this.mailHost = mailHost;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Transactional
    public OtpResult request(OtpRequest request) {
        String identifier = normalize(request.getIdentifier());
        validateIdentifier(identifier);
        Challenge previous = challenges.get(identifier);
        long now = Instant.now().getEpochSecond();
        if (previous != null && previous.resendAvailableAt() > now) {
            throw new IllegalArgumentException("Please wait " + (previous.resendAvailableAt() - now)
                    + " seconds before requesting another OTP.");
        }

        User user = findOrCreateUser(identifier);
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        challenges.put(identifier, new Challenge(user.getId(), code, now + OTP_TTL_SECONDS,
                now + RESEND_DELAY_SECONDS));
        deliverOtp(identifier, code);
        if (exposeDevelopmentCode) {
            log.info("OTP generated for {}. Development OTP: {}", mask(identifier), code);
        } else {
            log.info("OTP generated for {}. Connect an SMS/WhatsApp provider to deliver it.", mask(identifier));
        }
        return new OtpResult(user.getId(), mask(identifier), RESEND_DELAY_SECONDS,
            exposeDevelopmentCode ? code : null);
    }

    @Transactional(readOnly = true)
    public AuthResponse verify(OtpVerifyRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        String identifier = normalize(request.getIdentifier());
        Challenge challenge = challenges.get(identifier);
        long now = Instant.now().getEpochSecond();
        if (challenge == null || challenge.expiresAt() < now || !challenge.code().equals(request.getOtp().trim())) {
            throw new InvalidCredentialsException("Invalid or expired OTP.");
        }
        User user = userRepository.findById(challenge.userId())
                .orElseThrow(() -> new InvalidCredentialsException("User account was not found."));
        var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))).build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        challenges.remove(identifier);

        AuthResponse response = new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                "OTP verified. You are now logged in.");
        response.setPhone(user.getPhone());
        return response;
    }

    private User findOrCreateUser(String identifier) {
        User existing = identifier.matches("\\d{10}")
                ? userRepository.findByPhone(identifier).orElse(null)
                : userRepository.findByEmail(identifier).orElse(null);
        if (existing != null) return existing;

        String email = identifier.matches("\\d{10}") ? identifier + "@otp.local" : identifier;
        User stub = new User("New Buyer", email, passwordEncoder.encode(UUID.randomUUID().toString()), Role.BUYER);
        if (identifier.matches("\\d{10}")) stub.setPhone(identifier);
        return userRepository.save(stub);
    }

    private void deliverOtp(String identifier, String code) {
        if (identifier.matches("\\d{10}")) {
            if (!exposeDevelopmentCode) {
                throw new IllegalStateException("SMS or WhatsApp delivery is not configured. Set OTP_DEV_CODE=true for local testing.");
            }
            return;
        }
        if (mailSender == null || mailHost.isBlank()) {
            if (!exposeDevelopmentCode) {
                throw new IllegalStateException("Email delivery is not configured. Set SMTP properties or OTP_DEV_CODE=true for local testing.");
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(identifier);
        message.setSubject("SS Supermarket verification code");
        message.setText("Your SS Supermarket OTP is " + code + ". It expires in 5 minutes.");
        mailSender.send(message);
    }

    private String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }

    private void validateIdentifier(String identifier) {
        if (!identifier.matches("\\d{10}") && !identifier.contains("@")) {
            throw new IllegalArgumentException("Enter a valid 10-digit phone number or email address.");
        }
    }

    private String mask(String identifier) {
        if (identifier.matches("\\d{10}")) return "******" + identifier.substring(6);
        int at = identifier.indexOf('@');
        return identifier.substring(0, Math.min(2, at)) + "***" + identifier.substring(at);
    }

    private record Challenge(Long userId, String code, long expiresAt, long resendAvailableAt) { }

    public record OtpResult(Long userId, String maskedIdentifier, long resendAfterSeconds, String developmentOtp) { }
}
