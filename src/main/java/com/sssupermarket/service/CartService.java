package com.sssupermarket.service;

import com.sssupermarket.dto.CartItemResponse;
import com.sssupermarket.dto.CartResponse;
import com.sssupermarket.entity.CartItem;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.CartItemNotFoundException;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.CartItemRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItemResponse> items = cartItemRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(CartItemResponse::fromEntity)
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalItems, totalAmount);
    }

    @Transactional(readOnly = true)
    public long getCartCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    @Transactional
    public CartResponse addItem(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId + "."));
        CartItem existing = cartItemRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (newQuantity > product.getStock()) {
            throw new InsufficientStockException("Only " + product.getStock() + " unit(s) of "
                    + product.getName() + " are available in stock.");
        }
        if (existing == null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                throw new IllegalStateException("Authenticated user not found.");
            }
            cartItemRepository.save(new CartItem(user, product, quantity));
        } else {
            existing.setQuantity(newQuantity);
            cartItemRepository.save(existing);
        }
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found."));
        if (quantity > item.getProduct().getStock()) {
            throw new InsufficientStockException("Only " + item.getProduct().getStock()
                    + " unit(s) of " + item.getProduct().getName() + " are available in stock.");
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found."));
        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
