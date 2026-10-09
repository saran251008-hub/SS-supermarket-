package com.sssupermarket.controller;

import com.sssupermarket.entity.OrderStatus;
import com.sssupermarket.entity.Role;
import com.sssupermarket.service.CategoryService;
import com.sssupermarket.service.OrderService;
import com.sssupermarket.service.ProductService;
import com.sssupermarket.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("users", userService.listAllUsers());
        model.addAttribute("products", productService.getAllProductsAdmin());
        model.addAttribute("orders", orderService.listAllOrders());
        model.addAttribute("roles", Role.values());
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam Role role,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "User #" + id + " role updated to " + role.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update user role: " + e.getMessage());
        }
        return "redirect:/admin#users";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " status updated to " + status.name());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update order status: " + e.getMessage());
        }
        return "redirect:/admin#orders";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id, null, true);
            redirectAttributes.addFlashAttribute("successMessage", "Product #" + id + " removed/deactivated by admin.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove product: " + e.getMessage());
        }
        return "redirect:/admin#products";
    }
}
