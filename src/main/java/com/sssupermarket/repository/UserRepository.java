package com.sssupermarket.repository;

import com.sssupermarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    java.util.List<User> findAllByOrderByCreatedAtDesc();
}
