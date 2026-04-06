package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    List<PaymentCard> findByUserId(Long userId);
}
