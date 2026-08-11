package com.sssupermarket.service;

import com.sssupermarket.dto.OrderItemResponse;
import com.sssupermarket.dto.OrderResponse;
import com.sssupermarket.entity.CartItem;
import com.sssupermarket.entity.Order;
import com.sssupermarket.entity.OrderItem;
import com.sssupermarket.entity.OrderStatus;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.User;
import com.sssupermarket.exception.EmptyCartException;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.exception.OrderNotFoundException;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.CartItemRepository;
import com.sssupermarket.repository.OrderRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Your cart is empty. Add products before checking out.");
        }

        Order order = new Order(user, BigDecimal.ZERO, OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product " + item.getProduct().getName() + " is no longer available."));
            if (item.getQuantity() > product.getStock()) {
                throw new InsufficientStockException("Insufficient stock for " + product.getName()
                        + ". Only " + product.getStock() + " unit(s) left.");
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(order, product, item.getQuantity(), product.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found."));
        OrderResponse response = OrderResponse.fromEntity(order);
        response.setItems(order.getOrderItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList());
        return response;
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalAmount();
    }
}
