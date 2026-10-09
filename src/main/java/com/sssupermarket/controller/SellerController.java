package com.sssupermarket.controller;

import com.sssupermarket.entity.User;
import com.sssupermarket.dto.OrderItemResponse;
import com.sssupermarket.dto.OrderResponse;
import com.sssupermarket.repository.OrderRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CurrentUser currentUser;

    public SellerController(ProductRepository productRepository, OrderRepository orderRepository,
                            CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        User seller = currentUser.get();
        model.addAttribute("sellerName", seller.getName());
        model.addAttribute("productCount", productRepository.countBySellerId(seller.getId()));
        model.addAttribute("memberSince", seller.getCreatedAt());
        model.addAttribute("orderCount", orderRepository.countOrdersForSeller(seller.getId()));
        model.addAttribute("revenue", orderRepository.sumRevenueForSeller(seller.getId()));
        model.addAttribute("recentOrders", orderRepository.findRecentOrdersForSeller(seller.getId())
            .stream()
            .limit(8)
            .map(order -> {
                OrderResponse response = OrderResponse.fromEntity(order);
                response.setItems(order.getOrderItems().stream()
                    .map(OrderItemResponse::fromEntity)
                    .toList());
                return response;
            })
            .toList());
        return "seller";
    }
}
