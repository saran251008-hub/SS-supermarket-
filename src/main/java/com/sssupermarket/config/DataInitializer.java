package com.sssupermarket.config;

import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public static final String ADMIN_EMAIL = "admin@sssupermarket.com";
    public static final String ADMIN_PASSWORD = "Admin@123";

    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository,
                                          ProductRepository productRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
                userRepository.save(new User("SS Supermarket Admin", ADMIN_EMAIL,
                        passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
                log.info("Created default admin account: {}", ADMIN_EMAIL);
            }

            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                        new Product("Laptop",
                                "High performance laptop with 16 GB RAM and 512 GB SSD. Perfect for work and study.",
                                new BigDecimal("45000"), "Electronics",
                                "/images/products/laptop.svg", 10, null),
                        new Product("Smartphone",
                                "6.5 inch AMOLED display smartphone with 128 GB storage and dual camera.",
                                new BigDecimal("18000"), "Electronics",
                                "/images/products/smartphone.svg", 20, null),
                        new Product("Headphones",
                                "Wireless over-ear headphones with active noise cancellation and 30 hour battery.",
                                new BigDecimal("2500"), "Accessories",
                                "/images/products/headphones.svg", 30, null),
                        new Product("Keyboard",
                                "Mechanical keyboard with backlight and quiet switches for comfortable typing.",
                                new BigDecimal("1500"), "Accessories",
                                "/images/products/keyboard.svg", 25, null),
                        new Product("Smart Watch",
                                "Smart watch with heart rate monitor, GPS and 7 day battery life.",
                                new BigDecimal("5000"), "Wearables",
                                "/images/products/smartwatch.svg", 15, null)
                ));
                log.info("Seeded {} sample products.", productRepository.count());
            }
        };
    }
}
