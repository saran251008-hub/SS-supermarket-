package com.sssupermarket.controller;

import com.sssupermarket.dto.OrderResponse;
import com.sssupermarket.entity.Order;
import com.sssupermarket.dto.CheckoutRequest;
import jakarta.validation.Valid;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderApiController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder() {
        Order order = orderService.createOrder(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.getOrderForUser(currentUser.getId(), order.getId()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        Order order = orderService.checkout(currentUser.getId(), request.getAddressId(), request.getPaymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getOrderForUser(currentUser.getId(), order.getId()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders() {
        return ResponseEntity.ok(orderService.getOrdersForUser(currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForUser(currentUser.getId(), id));
    }
}
