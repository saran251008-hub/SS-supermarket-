package com.sssupermarket.service;

import com.sssupermarket.dto.ProductResponse;
import com.sssupermarket.entity.Product;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String search, String category) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        List<Product> products;
        if (hasSearch && hasCategory) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByCreatedAtDesc(
                    search.trim(), category.trim());
        } else if (hasSearch) {
            products = productRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim());
        } else if (hasCategory) {
            products = productRepository.findByCategoryIgnoreCaseOrderByCreatedAtDesc(category.trim());
        } else {
            products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return products.stream().map(ProductResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        List<ProductResponse> all = listProducts(null, null);
        return all.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return ProductResponse.fromEntity(getProductEntity(id));
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
}
