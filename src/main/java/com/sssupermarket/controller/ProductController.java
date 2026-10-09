package com.sssupermarket.controller;

import com.sssupermarket.service.ProductService;
import com.sssupermarket.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(defaultValue = "false") boolean includeChildren,
                       Model model) {
                model.addAttribute("products", categoryId == null ? productService.listProducts(search, category)
                    : productService.searchProducts(search, categoryId, includeChildren, 0, 20).getContent());
                model.addAttribute("categoryTree", categoryService.tree());
                model.addAttribute("categoryOptions", categoryService.flatTree());
                model.addAttribute("breadcrumb", categoryId == null ? List.of() : categoryService.breadcrumb(categoryId));
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("selectedCategoryId", categoryId);
        return "products";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProduct(id));
        return "product-details";
    }
}
