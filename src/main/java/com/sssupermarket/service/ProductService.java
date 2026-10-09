package com.sssupermarket.service;

import com.sssupermarket.dto.ProductFormRequest;
import com.sssupermarket.dto.ProductResponse;
import com.sssupermarket.entity.Category;
import com.sssupermarket.entity.Product;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.CartItemRepository;
import com.sssupermarket.repository.CategoryRepository;
import com.sssupermarket.repository.OrderItemRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          CategoryService categoryService,
                          CartItemRepository cartItemRepository,
                          OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
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

    @Transactional
    public ProductResponse createProduct(ProductFormRequest request, Long sellerId) {
        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSellerId(sellerId);
        product.setBrand(request.getBrand() != null && !request.getBrand().isBlank() ? request.getBrand().trim() : "SS Supermarket");

        String imageUrl = request.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = "/images/products/default.jpg";
        }
        product.setImageUrl(imageUrl.trim());

        Category categoryEntity = null;
        if (request.getCategoryId() != null) {
            categoryEntity = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        if (categoryEntity == null && request.getCategory() != null && !request.getCategory().isBlank()) {
            categoryEntity = categoryRepository.findByNameIgnoreCase(request.getCategory().trim()).orElse(null);
        }

        if (categoryEntity != null) {
            product.setCategoryEntity(categoryEntity);
            product.setCategory(categoryEntity.getName());
        } else {
            product.setCategory(request.getCategory() != null && !request.getCategory().isBlank() ? request.getCategory().trim() : "General");
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductFormRequest request, Long sellerId, boolean isAdmin) {
        Product product = getProductEntity(productId);
        if (!isAdmin && (product.getSellerId() == null || !product.getSellerId().equals(sellerId))) {
            throw new AccessDeniedException("You do not have permission to modify this product.");
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        if (request.getBrand() != null && !request.getBrand().isBlank()) {
            product.setBrand(request.getBrand().trim());
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl().trim());
        }

        Category categoryEntity = null;
        if (request.getCategoryId() != null) {
            categoryEntity = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        if (categoryEntity == null && request.getCategory() != null && !request.getCategory().isBlank()) {
            categoryEntity = categoryRepository.findByNameIgnoreCase(request.getCategory().trim()).orElse(null);
        }

        if (categoryEntity != null) {
            product.setCategoryEntity(categoryEntity);
            product.setCategory(categoryEntity.getName());
        } else if (request.getCategory() != null && !request.getCategory().isBlank()) {
            product.setCategory(request.getCategory().trim());
        }

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long productId, Long sellerId, boolean isAdmin) {
        Product product = getProductEntity(productId);
        if (!isAdmin && (product.getSellerId() == null || !product.getSellerId().equals(sellerId))) {
            throw new AccessDeniedException("You do not have permission to delete this product.");
        }

        cartItemRepository.deleteByProductId(productId);

        if (orderItemRepository.existsByProductId(productId)) {
            product.setStock(0);
            productRepository.save(product);
        } else {
            productRepository.delete(product);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsAdmin() {
        return productRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
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
