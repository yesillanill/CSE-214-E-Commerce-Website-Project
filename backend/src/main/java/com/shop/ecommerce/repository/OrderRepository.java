package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Order;
import com.shop.ecommerce.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser(User user, Pageable pageable);
    List<Order> findByUser(User user);

    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    Page<Order> findByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shipment JOIN o.orderItems oi WHERE oi.product.store.id = :storeId")
    Page<Order> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shipment JOIN o.orderItems oi WHERE oi.product.store.id = :storeId AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    Page<Order> findByStoreIdAndDateRange(@Param("storeId") Long storeId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.user.id = :userId")
    java.math.BigDecimal sumGrandTotalByUserId(@Param("userId") Long userId);
}
