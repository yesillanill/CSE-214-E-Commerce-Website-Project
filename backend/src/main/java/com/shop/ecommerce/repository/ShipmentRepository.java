package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);

    @Query("SELECT DISTINCT s FROM Shipment s JOIN s.order o JOIN o.orderItems oi WHERE oi.product.store.id = :storeId")
    Page<Shipment> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);
}
