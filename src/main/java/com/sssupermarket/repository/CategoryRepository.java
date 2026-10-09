package com.sssupermarket.repository;

import com.sssupermarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNullAndActiveTrueOrderByName();
    List<Category> findByParentIdAndActiveTrueOrderByName(Long parentId);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
