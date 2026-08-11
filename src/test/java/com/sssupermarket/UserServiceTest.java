package com.sssupermarket;

import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.InvalidRoleException;
import com.sssupermarket.exception.UserAlreadyExistsException;
import com.sssupermarket.repository.UserRepository;
import com.sssupermarket.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest validRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test Buyer");
        request.setEmail(email);
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole(Role.BUYER);
        return request;
    }

    @Test
    void register_storesBcryptHashAndNotPlainText() {
        User user = userService.register(validRequest("buyer@example.com"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.BUYER);
    }

    @Test
    void register_withExistingEmail_throws() {
        userService.register(validRequest("duplicate@example.com"));

        assertThatThrownBy(() -> userService.register(validRequest("duplicate@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_asAdmin_throws() {
        RegisterRequest request = validRequest("admin-wannabe@example.com");
        request.setRole(Role.ADMIN);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void register_withMismatchedPassword_throws() {
        RegisterRequest request = validRequest("mismatch@example.com");
        request.setConfirmPassword("different123");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByEmail_normalizesCase() {
        userService.register(validRequest("CaseTest@Example.com"));

        User found = userService.findByEmail("casetest@example.com");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("casetest@example.com");
        assertThat(userRepository.existsByEmail("casetest@example.com")).isTrue();
    }
}
