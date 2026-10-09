package com.sssupermarket.repository;

import com.sssupermarket.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    BigDecimal sumTotalAmount();

        @Query("select distinct o from Order o join fetch o.orderItems oi join fetch oi.product p "
            + "where p.sellerId = :sellerId order by o.createdAt desc")
        List<Order> findRecentOrdersForSeller(@Param("sellerId") Long sellerId);

        @Query("select count(distinct o.id) from Order o join o.orderItems oi "
            + "where oi.product.sellerId = :sellerId")
        long countOrdersForSeller(@Param("sellerId") Long sellerId);

        @Query("select coalesce(sum(oi.price * oi.quantity), 0) from OrderItem oi "
            + "where oi.product.sellerId = :sellerId")
        BigDecimal sumRevenueForSeller(@Param("sellerId") Long sellerId);

        List<Order> findAllByOrderByCreatedAtDesc();
}
