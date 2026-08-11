package com.sssupermarket.repository;

import com.sssupermarket.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    List<CartItem> findByUserIdOrderByIdAsc(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
