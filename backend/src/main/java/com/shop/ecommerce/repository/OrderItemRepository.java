package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.store.id = :storeId AND oi.order.id = :orderId")
    List<OrderItem> findByStoreIdAndOrderId(@Param("storeId") Long storeId, @Param("orderId") Long orderId);
}
