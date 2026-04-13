// Ödeme sonucu yanıt DTO'su
// Tüm ödeme yöntemleri için ortak yanıt yapısı
// checkoutUrl: Coinbase kripto ödemesi için yönlendirme URL'i
// approvalUrl: PayPal onay sayfası URL'i
package com.shop.ecommerce.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private String status;
    private String providerResponse;

    // Coinbase kripto ödemesi için checkout URL'i
    private String checkoutUrl;

    // PayPal ödeme onay sayfası URL'i
    private String approvalUrl;

    // Kullanıcıya gösterilecek mesaj
    private String message;
}
