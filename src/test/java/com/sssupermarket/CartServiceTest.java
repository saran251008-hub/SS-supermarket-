package com.sssupermarket;

import com.sssupermarket.dto.CartResponse;
import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.service.CartService;
import com.sssupermarket.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    private Long buyerId;
    private Product laptop;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Cart Buyer");
        request.setEmail("cart-buyer-" + System.nanoTime() + "@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole(Role.BUYER);
        User buyer = userService.register(request);
        buyerId = buyer.getId();

        laptop = productRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("Laptop").get(0);
        keyboard = productRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc("Keyboard").get(0);
    }

    @Test
    void addItem_createsAndIncrementsQuantity() {
        CartResponse cart = cartService.addItem(buyerId, laptop.getId(), 2);

        assertThat(cart.getTotalItems()).isEqualTo(2);

        cart = cartService.addItem(buyerId, laptop.getId(), 1);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.getItems().get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void addItem_quantityAboveStock_throws() {
        assertThatThrownBy(() -> cartService.addItem(buyerId, laptop.getId(), 999))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void addItem_unknownProduct_throws() {
        assertThatThrownBy(() -> cartService.addItem(buyerId, 999999L, 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateQuantity_and_removeItem() {
        cartService.addItem(buyerId, laptop.getId(), 2);
        Long itemId = cartService.getCart(buyerId).getItems().get(0).getId();

        CartResponse updated = cartService.updateQuantity(buyerId, itemId, 5);
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(updated.getTotalItems()).isEqualTo(5);

        CartResponse afterRemove = cartService.removeItem(buyerId, itemId);
        assertThat(afterRemove.isEmpty()).isTrue();
    }

    @Test
    void updateQuantity_aboveStock_throws() {
        cartService.addItem(buyerId, laptop.getId(), 1);
        Long itemId = cartService.getCart(buyerId).getItems().get(0).getId();

        assertThatThrownBy(() -> cartService.updateQuantity(buyerId, itemId, 999))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void totalAmount_calculatedOnBackend() {
        cartService.addItem(buyerId, laptop.getId(), 1);
        cartService.addItem(buyerId, keyboard.getId(), 2);

        CartResponse cart = cartService.getCart(buyerId);

        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getTotalItems()).isEqualTo(3);
        BigDecimal expected = laptop.getPrice()
                .add(keyboard.getPrice().multiply(BigDecimal.valueOf(2)));
        assertThat(cart.getTotalAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void clearCart_removesAllItems() {
        cartService.addItem(buyerId, laptop.getId(), 1);
        cartService.addItem(buyerId, keyboard.getId(), 1);

        cartService.clearCart(buyerId);

        assertThat(cartService.getCart(buyerId).isEmpty()).isTrue();
    }

    @Test
    void getCartCount_countsRows() {
        assertThat(cartService.getCartCount(buyerId)).isZero();

        cartService.addItem(buyerId, laptop.getId(), 3);
        cartService.addItem(buyerId, keyboard.getId(), 2);

        assertThat(cartService.getCartCount(buyerId)).isEqualTo(2);
    }

    @Test
    void twoItemsSameProduct_neverDuplicated() {
        cartService.addItem(buyerId, laptop.getId(), 1);
        cartService.addItem(buyerId, laptop.getId(), 1);
        cartService.addItem(buyerId, laptop.getId(), 1);

        CartResponse cart = cartService.getCart(buyerId);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(List.of()).isEmpty();
    }
}
