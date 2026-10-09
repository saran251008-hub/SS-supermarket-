package com.sssupermarket.service;

import com.sssupermarket.dto.CategoryResponse;
import com.sssupermarket.entity.Category;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final com.sssupermarket.repository.ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           com.sssupermarket.repository.ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> tree() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderByName().stream()
                .map(this::toTree).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> flatTree() {
        List<CategoryResponse> result = new ArrayList<>();
        tree().forEach(root -> flatten(root, result));
        return result;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> children(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderByName(parentId).stream()
                .map(CategoryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse subtree(Long id) {
        return toTree(get(id));
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Category not found with id " + id + "."));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> topLevel() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderByName().stream()
                .map(CategoryResponse::fromEntity).toList();
    }

    @Transactional
    public CategoryResponse update(Long id, String name, String description, String imageUrl, Long parentId,
                                   boolean active) {
        Category category = get(id);
        category.setName(name.trim());
        category.setDescription(description);
        category.setImageUrl(imageUrl);
        category.setActive(active);
        Category parent = parentId == null ? null : get(parentId);
        if (parent == category) throw new IllegalArgumentException("A category cannot be its own parent.");
        category.setParent(parent);
        category.setLevel(parent == null ? 0 : parent.getLevel() + 1);
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = get(id);
        if (!category.getChildren().isEmpty() || productRepository.existsByCategoryEntityId(id)) {
            throw new IllegalArgumentException("Category must have no children or products before deletion.");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<Long> descendantIds(Long id) {
        Category root = get(id);
        List<Long> ids = new ArrayList<>();
        collectIds(root, ids);
        return ids;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> breadcrumb(Long id) {
        List<CategoryResponse> result = new ArrayList<>();
        Category current = get(id);
        while (current != null) {
            result.add(0, CategoryResponse.fromEntity(current));
            current = current.getParent();
        }
        return result;
    }

    @Transactional
    public Category save(String name, String description, String imageUrl, Long parentId) {
        Category category = new Category();
        category.setName(name.trim());
        category.setSlug(slug(name));
        category.setDescription(description);
        category.setImageUrl(imageUrl);
        if (parentId != null) {
            Category parent = get(parentId);
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(0);
        }
        return categoryRepository.save(category);
    }

    private CategoryResponse toTree(Category category) {
        CategoryResponse response = CategoryResponse.fromEntity(category);
        response.setChildren(category.getChildren().stream()
                .filter(child -> Boolean.TRUE.equals(child.getActive()))
                .map(this::toTree).toList());
        return response;
    }

    private void collectIds(Category category, List<Long> ids) {
        ids.add(category.getId());
        category.getChildren().stream().filter(child -> Boolean.TRUE.equals(child.getActive()))
                .forEach(child -> collectIds(child, ids));
    }

    private void flatten(CategoryResponse category, List<CategoryResponse> result) {
        String originalName = category.getName();
        category.setName("  ".repeat(Math.max(0, category.getLevel())) + originalName);
        result.add(category);
        category.getChildren().forEach(child -> flatten(child, result));
    }

    private String slug(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
