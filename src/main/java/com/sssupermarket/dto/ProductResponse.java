package com.sssupermarket.dto;

import com.sssupermarket.entity.Product;

import java.math.BigDecimal;

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String brand;
    private BigDecimal price;
    private String category;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private Integer stock;
    private Long sellerId;
    private String sellerName;
    private String sellerPhone;

    public ProductResponse() {
    }

    public static ProductResponse fromEntity(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setBrand(product.getBrand());
        response.setPrice(product.getPrice());
        if (product.getCategoryEntity() != null) {
            String actualCategoryName = product.getCategoryEntity().getName();
            response.setCategory(product.getCategory() != null && !product.getCategory().isBlank()
                    ? product.getCategory() : actualCategoryName);
            response.setCategoryId(product.getCategoryEntity().getId());
            response.setCategoryName(actualCategoryName);
        } else {
            response.setCategory(product.getCategory());
        }
        response.setImageUrl(product.getImageUrl());
        response.setStock(product.getStock());
        response.setSellerId(product.getSellerId());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerPhone() {
        return sellerPhone;
    }

    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }
}
