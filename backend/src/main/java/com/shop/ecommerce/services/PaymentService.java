// Ana ödeme yönlendirici servisi
// Provider alanına göre ilgili ödeme servisini çağırır (Stripe, PayPal, Coinbase, COD)
// Sonucu payments tablosuna kaydeder
package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.entities.Order;
import com.shop.ecommerce.entities.Payment;
import com.shop.ecommerce.entities.PaymentCard;
import com.shop.ecommerce.enums.PaymentMethod;
import com.shop.ecommerce.enums.PaymentProvider;
import com.shop.ecommerce.enums.PaymentStatus;
import com.shop.ecommerce.exceptionHandler.PaymentException;
import com.shop.ecommerce.repository.OrderRepository;
import com.shop.ecommerce.repository.PaymentCardRepository;
import com.shop.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final StripeService stripeService;
    private final PayPalService payPalService;
    private final CoinbaseService coinbaseService;
    private final CodService codService;
    private final PaymentRepository paymentRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final OrderRepository orderRepository;

    /**
     * Provider'a göre ödeme işlemini yönlendirir ve sonucu veritabanına kaydeder
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Sipariş kontrolü
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new PaymentException("Sipariş bulunamadı: " + request.getOrderId()));

        // Kayıtlı kart ile ödeme yapılıyorsa token'ı al
        if (request.getSavedCardId() != null) {
            PaymentCard savedCard = paymentCardRepository.findById(request.getSavedCardId())
                    .orElseThrow(() -> new PaymentException("Kayıtlı kart bulunamadı"));
            request.setCardToken(savedCard.getProviderToken());
            request.setCardLastFour(savedCard.getCardNumber());
        }

        // Provider'a göre ödeme işlemini yönlendir
        PaymentResponse response;
        PaymentProvider provider;
        PaymentMethod paymentMethod;
        PaymentStatus paymentStatus;

        switch (request.getProvider().toUpperCase()) {
            case "STRIPE" -> {
                response = stripeService.createPaymentIntent(request);
                provider = PaymentProvider.STRIPE;
                paymentMethod = PaymentMethod.STRIPE;
                paymentStatus = "succeeded".equals(response.getStatus()) ? PaymentStatus.SUCCESS : PaymentStatus.PENDING;
            }
            case "PAYPAL" -> {
                response = payPalService.createOrder(request);
                provider = PaymentProvider.PAYPAL;
                paymentMethod = PaymentMethod.PAYPAL;
                paymentStatus = PaymentStatus.PENDING; // PayPal onayı bekleniyor
            }
            case "CRYPTO" -> {
                response = coinbaseService.createCharge(request);
                provider = PaymentProvider.CRYPTO;
                paymentMethod = PaymentMethod.CRYPTO;
                paymentStatus = PaymentStatus.PENDING; // Kripto ödemesi bekleniyor
            }
            case "COD" -> {
                response = codService.processPayment(request);
                provider = PaymentProvider.COD;
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY;
                paymentStatus = PaymentStatus.AWAITING_DELIVERY;
            }
            default -> throw new PaymentException("Geçersiz ödeme sağlayıcı: " + request.getProvider());
        }

        // Ödeme kaydını oluştur veya güncelle
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(new Payment());
        payment.setOrder(order);
        payment.setAmount(BigDecimal.valueOf(request.getAmount()));
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentProvider(provider);
        payment.setPaymentStatus(paymentStatus);
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        payment.setProviderTransactionId(response.getTransactionId());
        payment.setNotes(request.getDeliveryNotes());

        // Başarılı ödemelerde paid_at set et
        if (paymentStatus == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
        }

        // Kart ilişkilendirmesi
        if (request.getSavedCardId() != null) {
            PaymentCard card = paymentCardRepository.findById(request.getSavedCardId()).orElse(null);
            payment.setPaymentCard(card);
        }

        paymentRepository.save(payment);

        // Kart kaydetme isteği varsa (Stripe)
        if (Boolean.TRUE.equals(request.getSaveCard()) && "STRIPE".equalsIgnoreCase(request.getProvider())
                && request.getCardToken() != null) {
            try {
                saveCardForUser(order.getUser().getId(), request);
            } catch (Exception e) {
                log.warn("Kart kaydetme başarısız ama ödeme tamamlandı: {}", e.getMessage());
            }
        }

        return response;
    }

    /**
     * PayPal ödeme onayı (capture)
     */
    @Transactional
    public PaymentResponse capturePayPalOrder(String paypalOrderId, Long orderId) {
        PaymentResponse response = payPalService.captureOrder(paypalOrderId);

        if ("COMPLETED".equals(response.getStatus())) {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new PaymentException("Ödeme kaydı bulunamadı"));
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setProviderTransactionId(paypalOrderId);
            paymentRepository.save(payment);
        }

        return response;
    }

    /**
     * Kapıda ödeme teslimat onayı
     * AWAITING_DELIVERY → SUCCESS olarak günceller
     */
    @Transactional
    public PaymentResponse confirmCodDelivery(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Ödeme kaydı bulunamadı: " + paymentId));

        if (payment.getPaymentStatus() != PaymentStatus.AWAITING_DELIVERY) {
            throw new PaymentException("Bu ödeme zaten onaylanmış veya farklı durumda: " + payment.getPaymentStatus());
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Kapıda ödeme teslim onayı: Payment ID={}", paymentId);

        return PaymentResponse.builder()
                .transactionId(payment.getProviderTransactionId())
                .status("SUCCESS")
                .message("Kapıda ödeme başarıyla teslim alındı ve onaylandı")
                .build();
    }

    /**
     * Kullanıcının ödeme geçmişini getirir
     */
    public List<Payment> getPaymentHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByPaidAtDesc(userId);
    }

    /**
     * Siparişe ait ödeme detayını getirir
     */
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Bu siparişe ait ödeme bulunamadı: " + orderId));
    }

    /**
     * Kart kaydetme (token tabanlı)
     * GÜVENLİK: Sadece token ve son 4 hane kaydedilir
     */
    private void saveCardForUser(Long userId, PaymentRequest request) {
        PaymentCard card = new PaymentCard();
        card.setUser(orderRepository.findById(request.getOrderId()).orElseThrow().getUser());
        card.setCardHolderName(request.getCardHolderName());
        card.setCardNumber(request.getCardLastFour());
        card.setExpiryMonth(request.getExpiryMonth());
        card.setExpiryYear(request.getExpiryYear());
        card.setCardType(request.getCardType());
        card.setPaymentProvider(PaymentProvider.STRIPE);
        card.setProviderToken(request.getCardToken());
        card.setIsActive(true);
        paymentCardRepository.save(card);
    }
}
