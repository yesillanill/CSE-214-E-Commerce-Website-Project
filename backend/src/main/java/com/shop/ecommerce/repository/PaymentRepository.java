// Ödeme veritabanı erişim katmanı
// Sipariş ve kullanıcı bazlı ödeme sorgulama yöntemleri
package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Sipariş ID'sine göre ödeme bul
    Optional<Payment> findByOrderId(Long orderId);

    // Kullanıcının tüm ödemelerini tarih sırasına göre getir
    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.paidAt DESC")
    List<Payment> findByUserIdOrderByPaidAtDesc(@Param("userId") Long userId);
}
