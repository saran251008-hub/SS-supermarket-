package com.sssupermarket.repository;

import com.sssupermarket.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCaseOrderByCreatedAtDesc(String category);

    List<Product> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Product> findByNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByCreatedAtDesc(String name, String category);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findDistinctCategories();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    long countBySellerId(Long sellerId);
}
