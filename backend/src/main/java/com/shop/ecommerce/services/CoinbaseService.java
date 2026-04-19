// Coinbase Commerce kripto ödeme servisi
// Kullanıcıyı Coinbase Commerce checkout sayfasına yönlendirir
// GÜVENLİK: API key server-side tutulur
package com.shop.ecommerce.services;

import com.shop.ecommerce.config.CoinbaseConfig;
import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.exceptionHandler.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinbaseService {

    private final CoinbaseConfig coinbaseConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Coinbase Commerce charge oluşturur
     * Demo/test ortamında mock ödeme olarak simüle edilir
     * checkoutUrl döndürülmez — frontend doğrudan başarılı olarak işler
     */
    public PaymentResponse createCharge(PaymentRequest request) {
        // GÜNCELLEME: Eski Coinbase Commerce API'si kapandığı için (Deprecated), 
        // test/demo ortamında ödemeyi doğrudan başarılı olarak simüle ediyoruz.
        String transactionId = "cb_" + System.currentTimeMillis();
        
        return PaymentResponse.builder()
                .message("Kripto ödeme başarıyla tamamlandı (demo)")
                .transactionId(transactionId)
                .status("succeeded")
                .build();
    }
}
