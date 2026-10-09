package com.sssupermarket.service;

import com.sssupermarket.dto.ProductResponse;
import com.sssupermarket.entity.Product;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, UserRepository userRepository,
                          CategoryService categoryService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String search, String category) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        List<Product> products = productRepository.searchLegacy(
                hasSearch ? search.trim() : "", hasCategory ? category.trim() : "");
        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        List<ProductResponse> all = listProducts(null, null);
        return all.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return toResponse(getProductEntity(id));
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

        @Transactional(readOnly = true)
        public Page<ProductResponse> searchProducts(String search, Long categoryId, boolean includeChildren,
                            int page, int size) {
        List<Long> categoryIds = categoryId == null ? null
            : (includeChildren ? categoryService.descendantIds(categoryId) : List.of(categoryId));
        return productRepository.searchProducts(search == null ? "" : search.trim(), categoryIds,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toResponse);
        }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = ProductResponse.fromEntity(product);
        if (product.getSellerId() != null) {
            userRepository.findById(product.getSellerId()).ifPresent(seller -> {
                response.setSellerName(seller.getName());
                if (seller.getPhone() != null && seller.getPhone().matches("\\d{10}")) {
                    response.setSellerPhone(seller.getPhone());
                }
            });
        }
        return response;
    }
}
