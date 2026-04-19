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

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shipment JOIN o.orderItems oi WHERE oi.product.store.id = :storeId AND " +
           "(CAST(o.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(oi.product.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchByStoreId(@Param("storeId") Long storeId, @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.user.id = :userId")
    java.math.BigDecimal sumGrandTotalByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p LEFT JOIN FETCH p.category LEFT JOIN FETCH o.shipment LEFT JOIN FETCH o.payment WHERE o.user.id = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.store LEFT JOIN FETCH o.shipment LEFT JOIN FETCH o.payment WHERE p.store.id = :storeId")
    List<Order> findAllByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o")
    java.math.BigDecimal sumAllGrandTotal();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);
}
