package com.sssupermarket.dto;

import com.sssupermarket.entity.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Integer level;
    private Long parentId;
    private List<CategoryResponse> children = new ArrayList<>();

    public static CategoryResponse fromEntity(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        response.slug = category.getSlug();
        response.description = category.getDescription();
        response.imageUrl = category.getImageUrl();
        response.level = category.getLevel();
        response.parentId = category.getParent() == null ? null : category.getParent().getId();
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public List<CategoryResponse> getChildren() { return children; }
    public void setChildren(List<CategoryResponse> children) { this.children = children; }
}
