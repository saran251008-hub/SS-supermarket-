package com.sssupermarket.service;

import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.InvalidRoleException;
import com.sssupermarket.exception.UserAlreadyExistsException;
import com.sssupermarket.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new InvalidRoleException("Registration as ADMIN is not allowed.");
        }
        if (request.getRole() == Role.SELLER
                && (request.getPhone() == null || request.getPhone().isBlank())) {
            throw new IllegalArgumentException("Seller phone number is required and must contain exactly 10 digits.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("An account with this email already exists.");
        }
        User user = new User(request.getName().trim(), email,
                passwordEncoder.encode(request.getPassword()), request.getRole());
        user.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }
}
