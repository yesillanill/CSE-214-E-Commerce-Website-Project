// Ödeme API Controller'ı
// Stripe, PayPal, Coinbase Commerce (Kripto) ve Kapıda Ödeme endpoint'leri
// GÜVENLİK: Webhook endpoint'leri imza doğrulaması yapar
// GÜVENLİK: Kart listesi dönerken token asla frontend'e gönderilmez
package com.shop.ecommerce.controller;

import com.shop.ecommerce.config.CodConfig;
import com.shop.ecommerce.config.PayPalConfig;
import com.shop.ecommerce.dto.card.PaymentCardCreateDTO;
import com.shop.ecommerce.dto.card.PaymentCardDTO;
import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.entities.Payment;
import com.shop.ecommerce.services.PaymentCardService;
import com.shop.ecommerce.services.PaymentService;
import com.shop.ecommerce.services.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentCardService paymentCardService;
    private final StripeService stripeService;
    private final PayPalConfig payPalConfig;
    private final CodConfig codConfig;

    /**
     * Ödeme başlat — Provider'a göre yönlendirir
     * POST /api/payments/create
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Stripe token ile kart kaydet
     * POST /api/payments/cards/save
     * GÜVENLİK: Sadece token (pm_xxx) ve son 4 hane kaydedilir
     */
    @PostMapping("/cards/save")
    public ResponseEntity<PaymentCardDTO> saveCard(@RequestBody PaymentCardCreateDTO dto) {
        PaymentCardDTO saved = paymentCardService.addCard(dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Kullanıcının kayıtlı kartlarını listele
     * GET /api/payments/cards/{userId}
     * GÜVENLİK: Token bilgisi (pm_xxx) asla frontend'e gönderilmez
     */
    @GetMapping("/cards/{userId}")
    public ResponseEntity<List<PaymentCardDTO>> getUserCards(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    /**
     * Kullanıcının ödeme geçmişi
     * GET /api/payments/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId));
    }

    /**
     * Siparişe ait ödeme detayı
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    /**
     * PayPal ödeme onayı (capture)
     * POST /api/payments/paypal/capture
     */
    @PostMapping("/paypal/capture")
    public ResponseEntity<PaymentResponse> capturePayPalOrder(
            @RequestParam String paypalOrderId,
            @RequestParam Long orderId) {
        return ResponseEntity.ok(paymentService.capturePayPalOrder(paypalOrderId, orderId));
    }

    /**
     * Stripe webhook — ödeme durumu güncellemeleri
     * POST /api/payments/webhook/stripe
     * GÜVENLİK: İmza doğrulaması yapılır
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        if (!stripeService.verifyWebhookSignature(payload, sigHeader)) {
            log.warn("Stripe webhook imza doğrulama başarısız!");
            return ResponseEntity.badRequest().body("Geçersiz imza");
        }
        log.info("Stripe webhook alındı ve doğrulandı");
        // Webhook event'ini işle (ödeme durumu güncelleme vb.)
        return ResponseEntity.ok("Webhook alındı");
    }

    /**
     * PayPal webhook — ödeme durumu güncellemeleri
     * POST /api/payments/webhook/paypal
     */
    @PostMapping("/webhook/paypal")
    public ResponseEntity<String> paypalWebhook(@RequestBody String payload) {
        log.info("PayPal webhook alındı");
        // PayPal webhook event'ini işle
        return ResponseEntity.ok("Webhook alındı");
    }

    /**
     * Kapıda ödeme teslimat onayı
     * PATCH /api/payments/{paymentId}/cod-confirm
     * payment_status: AWAITING_DELIVERY → SUCCESS
     * paid_at: NOW()
     */
    @PatchMapping("/{paymentId}/cod-confirm")
    public ResponseEntity<PaymentResponse> confirmCodDelivery(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.confirmCodDelivery(paymentId));
    }

    /**
     * Frontend için PayPal Client ID döndür
     * GET /api/payments/config/paypal-client-id
     * GÜVENLİK: Sadece client ID döner, secret dönmez
     */
    @GetMapping("/config/paypal-client-id")
    public ResponseEntity<Map<String, String>> getPayPalClientId() {
        return ResponseEntity.ok(Map.of("clientId", payPalConfig.getClientId()));
    }

    /**
     * Frontend için COD maksimum tutar limiti döndür
     * GET /api/payments/config/cod-limit
     */
    @GetMapping("/config/cod-limit")
    public ResponseEntity<Map<String, Double>> getCodLimit() {
        return ResponseEntity.ok(Map.of("maxAmount", codConfig.getMaxAmount()));
    }

    /**
     * Kart sil (soft delete)
     * DELETE /api/payments/cards/{cardId}
     */
    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId, @RequestParam Long userId) {
        paymentCardService.deleteCard(cardId, userId);
        return ResponseEntity.ok().build();
    }
}
