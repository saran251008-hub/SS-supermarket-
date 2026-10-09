package com.sssupermarket.controller;

import com.sssupermarket.dto.OrderItemResponse;
import com.sssupermarket.dto.OrderResponse;
import com.sssupermarket.dto.ProductFormRequest;
import com.sssupermarket.entity.User;
import com.sssupermarket.repository.OrderRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.CategoryService;
import com.sssupermarket.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CurrentUser currentUser;

    public SellerController(ProductRepository productRepository,
                            OrderRepository orderRepository,
                            ProductService productService,
                            CategoryService categoryService,
                            CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        User seller = currentUser.get();
        model.addAttribute("sellerName", seller.getName());
        model.addAttribute("sellerPhone", seller.getPhone());
        model.addAttribute("productCount", productRepository.countBySellerId(seller.getId()));
        model.addAttribute("memberSince", seller.getCreatedAt());
        model.addAttribute("orderCount", orderRepository.countOrdersForSeller(seller.getId()));
        model.addAttribute("revenue", orderRepository.sumRevenueForSeller(seller.getId()));
        model.addAttribute("sellerProducts", productService.getProductsBySeller(seller.getId()));
        model.addAttribute("categoryOptions", categoryService.flatTree());
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductFormRequest());
        }
        model.addAttribute("recentOrders", orderRepository.findRecentOrdersForSeller(seller.getId())
            .stream()
            .limit(10)
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

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductFormRequest form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productForm", bindingResult);
            redirectAttributes.addFlashAttribute("productForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix errors in the product form: "
                    + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/seller#products";
        }
        User seller = currentUser.get();
        productService.createProduct(form, seller.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Product '" + form.getName() + "' created and published to storefront!");
        return "redirect:/seller#products";
    }

    @PostMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id,
                              @Valid @ModelAttribute ProductFormRequest form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: "
                    + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/seller#products";
        }
        User seller = currentUser.get();
        boolean isAdmin = seller.getRole() != null && seller.getRole().name().equals("ADMIN");
        productService.updateProduct(id, form, seller.getId(), isAdmin);
        redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        return "redirect:/seller#products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        User seller = currentUser.get();
        boolean isAdmin = seller.getRole() != null && seller.getRole().name().equals("ADMIN");
        productService.deleteProduct(id, seller.getId(), isAdmin);
        redirectAttributes.addFlashAttribute("successMessage", "Product removed/deactivated successfully.");
        return "redirect:/seller#products";
    }
}
