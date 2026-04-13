// Kart bilgisi görüntüleme DTO'su (Frontend'e gönderilir)
// GÜVENLİK: providerToken asla bu DTO'da yer almaz, frontend'e gönderilmez
// GÜVENLİK: Sadece son 4 hane ve kart tipi gibi güvenli bilgiler döner
package com.shop.ecommerce.dto.card;

import lombok.Data;

@Data
public class PaymentCardDTO {
    private Long id;
    private String cardHolderName;
    private String lastFour; // Son 4 hane (örn: "4242")
    private String maskedCardNumber; // **** **** **** 4242 formatı
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardType;
    private String paymentProvider; // STRIPE, PAYPAL vs.
}
