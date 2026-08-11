package com.sssupermarket.controller;

import com.sssupermarket.entity.User;
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
    private final CurrentUser currentUser;

    public SellerController(ProductRepository productRepository, CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String dashboard(Model model) {
        User seller = currentUser.get();
        model.addAttribute("sellerName", seller.getName());
        model.addAttribute("productCount", productRepository.countBySellerId(seller.getId()));
        model.addAttribute("memberSince", seller.getCreatedAt());
        return "seller";
    }
}
