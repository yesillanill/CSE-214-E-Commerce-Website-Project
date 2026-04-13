// Ödeme kartı veritabanı erişim katmanı
// Kullanıcının aktif kartlarını sorgulama yöntemi
package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

    // Kullanıcının tüm kartlarını getir (eski uyumluluk için)
    List<PaymentCard> findByUserId(Long userId);

    // Kullanıcının sadece aktif kartlarını getir
    List<PaymentCard> findByUserIdAndIsActiveTrue(Long userId);
}
