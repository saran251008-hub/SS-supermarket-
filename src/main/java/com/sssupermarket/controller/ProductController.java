package com.sssupermarket.controller;

import com.sssupermarket.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       Model model) {
        model.addAttribute("products", productService.listProducts(search, category));
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        return "products";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProduct(id));
        return "product-details";
    }
}
