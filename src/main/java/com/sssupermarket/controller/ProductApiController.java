package com.sssupermarket.controller;
import com.sssupermarket.dto.ProductResponse;
import com.sssupermarket.service.ProductService;
import com.sssupermarket.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final CurrentUser currentUser;

    public ProductApiController(ProductService productService, CurrentUser currentUser) {
        this.productService = productService;
        this.currentUser = currentUser;
    }
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(defaultValue = "false") boolean includeChildren,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        if (categoryId != null) {
            return ResponseEntity.ok(productService.searchProducts(search, categoryId, includeChildren, page, size).getContent());
        }
        return ResponseEntity.ok(productService.listProducts(search, category));
    }

    @GetMapping("/personalized")
    public ResponseEntity<List<ProductResponse>> personalized() {
        currentUser.getId();
        return ResponseEntity.ok(productService.listProducts(null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
}
