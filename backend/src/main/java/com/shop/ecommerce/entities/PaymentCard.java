// PCI DSS uyumlu ödeme kartı entity'si
// GÜVENLİK: CVV hiçbir zaman veritabanına kaydedilmez
// GÜVENLİK: Kart numarası yerine sadece son 4 hane (lastFour) saklanır
// GÜVENLİK: Gerçek kart bilgisi Stripe tarafında tutulur, burada sadece token (pm_xxx) saklanır
package com.shop.ecommerce.entities;

import com.shop.ecommerce.enums.PaymentProvider;
import com.shop.ecommerce.enums.PaymentProviderConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "payment_cards")
public class PaymentCard {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "card_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    // GÜVENLİK: Sadece son 4 hane saklanır, tam kart numarası backend'e hiç gelmez
    @Column(name = "card_number", nullable = false, length = 4)
    private String cardNumber;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    // CVV ALANI KALDIRILDI — PCI DSS standardı gereği CVV asla saklanmaz
    // CVV sadece ödeme anında Stripe.js iframe'i üzerinden doğrudan Stripe'a iletilir

    @Column(name = "card_type")
    private String cardType; // VISA, MASTERCARD, TROY

    // Ödeme sağlayıcı: STRIPE, PAYPAL, CRYPTO, COD
    @Column(name = "payment_provider", length = 50)
    @Convert(converter = PaymentProviderConverter.class)
    private PaymentProvider paymentProvider;

    // Stripe PaymentMethod token'ı (pm_xxx)
    // GÜVENLİK: Bu token asla frontend'e gönderilmez
    @Column(name = "provider_token")
    private String providerToken;

    // Kartın aktif olup olmadığı
    @Column(name = "is_active")
    private Boolean isActive = true;
}
