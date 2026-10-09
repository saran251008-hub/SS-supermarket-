package com.sssupermarket.controller;

import com.sssupermarket.dto.CategoryRequest;
import com.sssupermarket.dto.CategoryResponse;
import com.sssupermarket.service.CategoryService;
import com.sssupermarket.service.ProductService;
import com.sssupermarket.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {
    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryApiController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping
    public List<CategoryResponse> topLevel() { return categoryService.topLevel(); }

    @GetMapping("/tree")
    public List<CategoryResponse> tree() { return categoryService.tree(); }

    @GetMapping("/{id}/children")
    public List<CategoryResponse> children(@PathVariable Long id) { return categoryService.children(id); }

    @GetMapping("/{id}/tree")
    public CategoryResponse subtree(@PathVariable Long id) {
        return categoryService.subtree(id);
    }

    @GetMapping("/{id}/breadcrumb")
    public List<CategoryResponse> breadcrumb(@PathVariable Long id) { return categoryService.breadcrumb(id); }

    @GetMapping("/{id}/products")
    public List<ProductResponse> products(@PathVariable Long id,
                                          @RequestParam(defaultValue = "true") boolean includeChildren,
                                          @RequestParam(required = false) String search) {
        return productService.searchProducts(search, id, includeChildren, 0, 100).getContent();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.fromEntity(
                categoryService.save(request.getName(), request.getDescription(), request.getImageUrl(), request.getParentId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request.getName(), request.getDescription(), request.getImageUrl(),
                request.getParentId(), true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
