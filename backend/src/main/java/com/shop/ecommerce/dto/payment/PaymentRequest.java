// Ödeme başlatma isteği DTO'su
// Tüm ödeme yöntemleri (Stripe, PayPal, Kripto, Kapıda Ödeme) için ortak istek yapısı
// GÜVENLİK: cardToken alanı Stripe'dan gelen pm_xxx token'ıdır, gerçek kart bilgisi DEĞİLDİR
package com.shop.ecommerce.dto.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private Double amount;
    private String currency;
    private Long orderId;

    // Ödeme sağlayıcı: "STRIPE", "PAYPAL", "CRYPTO", "COD"
    private String provider;

    // Stripe için: pm_xxx token'ı (Stripe Elements'ten gelir)
    // GÜVENLİK: Bu bir token'dır, gerçek kart numarası DEĞİLDİR
    private String cardToken;

    // Sadece son 4 hane (örn: "4242")
    private String cardLastFour;

    private String cardHolderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardType;

    // Kapıda ödeme için teslimat notu
    private String deliveryNotes;

    private String description;

    // Mevcut kayıtlı kart ile ödeme yapmak için
    private Long savedCardId;

    // Kartı kaydet checkbox'ı
    private Boolean saveCard;
}
