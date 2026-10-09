package com.sssupermarket.repository;

import com.sssupermarket.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        @Query("select p from Product p where (:category is null or :category = '' or lower(p.category) = lower(:category)) "
            + "and (:search is null or :search = '' or lower(p.name) like lower(concat('%', :search, '%')) "
            + "or lower(p.description) like lower(concat('%', :search, '%')) "
            + "or lower(coalesce(p.brand, '')) like lower(concat('%', :search, '%'))) "
            + "order by p.createdAt desc")
        List<Product> searchLegacy(@Param("search") String search, @Param("category") String category);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findDistinctCategories();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    long countBySellerId(Long sellerId);

    @Query("select p from Product p where (:search is null or :search = '' or "
            + "lower(p.name) like lower(concat('%', :search, '%')) or "
            + "lower(p.description) like lower(concat('%', :search, '%')) or "
            + "lower(coalesce(p.brand, '')) like lower(concat('%', :search, '%'))) "
            + "and (:categoryIds is null or p.categoryEntity.id in :categoryIds)")
    Page<Product> searchProducts(@Param("search") String search,
                                 @Param("categoryIds") List<Long> categoryIds,
                                 Pageable pageable);

    List<Product> findByCategoryEntityIdIn(List<Long> categoryIds);

    boolean existsByCategoryEntityId(Long categoryId);
}
