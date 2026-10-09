package com.sssupermarket;

import com.sssupermarket.dto.RegisterRequest;
import com.sssupermarket.entity.Order;
import com.sssupermarket.entity.OrderStatus;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.EmptyCartException;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.exception.OrderNotFoundException;
import com.sssupermarket.repository.CartItemRepository;
import com.sssupermarket.repository.OrderRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import com.sssupermarket.service.CartService;
import com.sssupermarket.service.OrderService;
import com.sssupermarket.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Map<String, Integer> INITIAL_STOCK = Map.of(
            "Laptop", 10, "Smartphone", 20, "Headphones", 30,
            "Keyboard", 25, "Smart Watch", 15);

    private Product laptop;
    private Product keyboard;

    @BeforeEach
    void resetStock() {
        for (Product product : productRepository.findAll()) {
            Integer initial = INITIAL_STOCK.get(product.getName());
            if (initial != null) {
                product.setStock(initial);
                productRepository.save(product);
            }
        }
        laptop = productRepository.findByName("Laptop").orElseThrow();
        keyboard = productRepository.findByName("Keyboard").orElseThrow();
    }

    private Long createBuyer(String prefix) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Order Buyer");
        request.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setRole(Role.BUYER);
        return userService.register(request).getId();
    }

    @Test
    void createOrder_createsOrderReducesStockAndClearsCart() {
        Long buyerId = createBuyer("success");

        cartService.addItem(buyerId, laptop.getId(), 2);
        cartService.addItem(buyerId, keyboard.getId(), 1);

        Order order = orderService.createOrder(buyerId);

        BigDecimal expectedTotal = laptop.getPrice().multiply(BigDecimal.valueOf(2))
                .add(keyboard.getPrice());
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getOrderItems().stream()
                .map(oi -> oi.getProduct().getName()))
                .contains("Laptop", "Keyboard");

        Product laptopAfter = productRepository.findById(laptop.getId()).orElseThrow();
        Product keyboardAfter = productRepository.findById(keyboard.getId()).orElseThrow();
        assertThat(laptopAfter.getStock()).isEqualTo(8);
        assertThat(keyboardAfter.getStock()).isEqualTo(24);

        assertThat(cartItemRepository.findByUserIdOrderByIdAsc(buyerId)).isEmpty();
    }

    @Test
    void createOrder_withEmptyCart_throws() {
        Long buyerId = createBuyer("empty");

        assertThatThrownBy(() -> orderService.createOrder(buyerId))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void createOrder_withInsufficientStock_rollsBack() {
        Long buyerId = createBuyer("stock");

        cartService.addItem(buyerId, laptop.getId(), 10);
        laptop.setStock(5);
        productRepository.save(laptop);

        assertThatThrownBy(() -> orderService.createOrder(buyerId))
                .isInstanceOf(InsufficientStockException.class);

        Product laptopAfter = productRepository.findById(laptop.getId()).orElseThrow();
        assertThat(laptopAfter.getStock()).isEqualTo(5);
        assertThat(cartItemRepository.findByUserIdOrderByIdAsc(buyerId)).hasSize(1);
        assertThat(orderRepository.findByUserIdOrderByCreatedAtDesc(buyerId)).isEmpty();
    }

    @Test
    void getOrdersForUser_returnsOnlyOwnOrders() {
        Long buyerId = createBuyer("own1");
        Long otherBuyerId = createBuyer("own2");

        cartService.addItem(buyerId, laptop.getId(), 1);
        Order order = orderService.createOrder(buyerId);

        List<com.sssupermarket.dto.OrderResponse> myOrders = orderService.getOrdersForUser(buyerId);
        List<com.sssupermarket.dto.OrderResponse> otherOrders = orderService.getOrdersForUser(otherBuyerId);

        assertThat(myOrders).hasSize(1);
        assertThat(myOrders.get(0).getId()).isEqualTo(order.getId());
        assertThat(otherOrders).isEmpty();
    }

    @Test
    void getOrderForUser_deniesOtherUsersOrder() {
        Long buyerId = createBuyer("deny1");
        Long otherBuyerId = createBuyer("deny2");

        cartService.addItem(buyerId, laptop.getId(), 1);
        Order order = orderService.createOrder(buyerId);

        assertThat(orderService.getOrderForUser(buyerId, order.getId()).getId()).isEqualTo(order.getId());

        assertThatThrownBy(() -> orderService.getOrderForUser(otherBuyerId, order.getId()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersForUser_includesOrderItems() {
        Long buyerId = createBuyer("items");

        cartService.addItem(buyerId, laptop.getId(), 1);
        cartService.addItem(buyerId, keyboard.getId(), 2);
        Order order = orderService.createOrder(buyerId);

        var response = orderService.getOrderForUser(buyerId, order.getId());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).extracting("productName").contains("Laptop", "Keyboard");
        assertThat(response.getItems().stream()
                .map(i -> i.getSubtotal()))
                .allMatch(subtotal -> subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void totalRevenue_sumsConfirmedOrders() {
        Long buyerId = createBuyer("revenue");

        BigDecimal before = orderService.getTotalRevenue();

        cartService.addItem(buyerId, keyboard.getId(), 1);
        Order order = orderService.createOrder(buyerId);

        assertThat(orderService.getTotalRevenue()).isEqualByComparingTo(before.add(order.getTotalAmount()));
        assertThat(userRepository.findById(buyerId)).isPresent();
    }
}
