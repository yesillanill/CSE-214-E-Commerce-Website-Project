// Ödeme kayıt entity'si
// Mevcut kolonlar (id, order, paymentMethod, paymentStatus, amount, paidAt) korunmuştur
// Yeni eklenen: paymentProvider, paymentCard, currency, providerTransactionId, notes
package com.shop.ecommerce.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shop.ecommerce.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "payments")
public class Payment {

    @EqualsAndHashCode.Include
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mevcut kolon — değiştirilmedi
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    // Mevcut kolon — değiştirilmedi
    @Column(name = "payment_method")
    @Convert(converter = PaymentMethodConverter.class)
    private PaymentMethod paymentMethod;

    // Mevcut kolon — değiştirilmedi
    @Column(name = "payment_status")
    @Convert(converter = PaymentStatusConverter.class)
    private PaymentStatus paymentStatus;

    // Mevcut kolon — değiştirilmedi
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    // Mevcut kolon — değiştirilmedi
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // === YENİ KOLONLAR ===

    // Ödeme sağlayıcı: STRIPE, PAYPAL, CRYPTO, COD
    @Column(name = "payment_provider", length = 50)
    @Convert(converter = PaymentProviderConverter.class)
    private PaymentProvider paymentProvider;

    // İlişkili kart (kripto ve kapıda ödeme için NULL olur)
    @ManyToOne
    @JoinColumn(name = "card_id")
    private PaymentCard paymentCard;

    // Para birimi (varsayılan: USD)
    @Column(name = "currency", length = 10)
    private String currency = "USD";

    // Stripe/PayPal/Coinbase'den gelen transaction ID
    // Kapıda ödeme için otomatik UUID üretilir
    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    // Kapıda ödeme teslimat notu
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
