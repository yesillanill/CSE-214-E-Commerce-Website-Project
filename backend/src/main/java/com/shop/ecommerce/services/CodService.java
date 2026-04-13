// Kapıda ödeme (Cash on Delivery) servisi
// Tutar limiti kontrolü hem bu serviste hem frontend'de yapılır
// Ödeme durumu AWAITING_DELIVERY olarak kaydedilir, teslimat sonrası SUCCESS'e güncellenir
package com.shop.ecommerce.services;

import com.shop.ecommerce.config.CodConfig;
import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.exceptionHandler.CodLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodService {

    private final CodConfig codConfig;

    /**
     * Kapıda ödeme işlemi
     * Tutar limitini kontrol eder ve UUID ile transaction ID üretir
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        // Tutar limiti kontrolü — hem frontend hem backend'de yapılır
        if (request.getAmount() > codConfig.getMaxAmount()) {
            throw new CodLimitExceededException(
                    String.format("Kapıda ödeme %.2f %s üzerindeki siparişler için geçerli değildir. Maksimum tutar: %.2f %s",
                            codConfig.getMaxAmount(),
                            request.getCurrency() != null ? request.getCurrency() : "USD",
                            codConfig.getMaxAmount(),
                            request.getCurrency() != null ? request.getCurrency() : "USD")
            );
        }

        // Kapıda ödeme için benzersiz transaction ID üret
        String transactionId = "COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Kapıda ödeme oluşturuldu: {} - Tutar: {} - Teslimat Notu: {}",
                transactionId, request.getAmount(), request.getDeliveryNotes());

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status("AWAITING_DELIVERY")
                .message("Siparişiniz onaylandı. Teslimat sırasında ödeme yapılacaktır.")
                .build();
    }
}
