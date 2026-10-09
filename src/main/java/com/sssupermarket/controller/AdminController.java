package com.sssupermarket.controller;

import com.sssupermarket.service.OrderService;
import com.sssupermarket.service.ProductService;
import com.sssupermarket.service.UserService;
import com.sssupermarket.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;

    public AdminController(UserService userService, ProductService productService, OrderService orderService,
                           CategoryService categoryService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalProducts", productService.countProducts());
        model.addAttribute("totalOrders", orderService.countOrders());
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());
        model.addAttribute("categoryOptions", categoryService.flatTree());
        return "admin";
    }
}
