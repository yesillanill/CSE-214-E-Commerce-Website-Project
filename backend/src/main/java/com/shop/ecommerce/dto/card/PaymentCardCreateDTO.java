// Token tabanlı kart kaydetme DTO'su
// GÜVENLİK: CVV ve tam kart numarası bu DTO'da YOKTUR
// GÜVENLİK: Frontend'den sadece Stripe token (pm_xxx) ve son 4 hane gelir
// GÜVENLİK: Gerçek kart bilgisi Stripe.js iframe'i üzerinden doğrudan Stripe'a gönderilir
package com.shop.ecommerce.dto.card;

import lombok.Data;

@Data
public class PaymentCardCreateDTO {
    private Long userId;
    private String cardHolderName;

    // Stripe PaymentMethod token'ı (pm_xxx)
    // GÜVENLİK: Bu gerçek kart numarası DEĞİL, Stripe'ın ürettiği token'dır
    private String cardToken;

    // Sadece son 4 hane (örn: "4242")
    private String lastFour;

    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardType;
    private String paymentProvider; // STRIPE varsayılan
}
