// Stripe ödeme servisi
// GÜVENLİK: Gerçek kart bilgisi bu servise GELMEZ, sadece Stripe token (pm_xxx) kullanılır
// GÜVENLİK: Stripe API çağrıları server-side yapılır, secret key frontend'e asla açılmaz
package com.shop.ecommerce.services;

import com.shop.ecommerce.config.StripeConfig;
import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.exceptionHandler.PaymentException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;

    /**
     * Stripe PaymentIntent oluşturur
     * Token ile ödeme yapar — gerçek kart bilgisi backend'e hiç gelmez
     */
    public PaymentResponse createPaymentIntent(PaymentRequest request) {
        try {
            // Tutar cent cinsinden olmalı (1 USD = 100 cent)
            long amountInCents = Math.round(request.getAmount() * 100);

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency() != null ? request.getCurrency().toLowerCase() : "usd")
                    .setDescription(request.getDescription() != null ? request.getDescription() : "E-Commerce Ödeme")
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    );

            // Yeni kart token'ı veya kayıtlı kart token'ı ile ödeme
            if (request.getCardToken() != null && !request.getCardToken().isEmpty()) {
                paramsBuilder.setPaymentMethod(request.getCardToken());
            }

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            log.info("Stripe PaymentIntent oluşturuldu: {}", paymentIntent.getId());

            return PaymentResponse.builder()
                    .transactionId(paymentIntent.getId())
                    .status(paymentIntent.getStatus())
                    .providerResponse(paymentIntent.getStatus())
                    .message("Stripe ödemesi başarıyla işlendi")
                    .build();

        } catch (StripeException e) {
            log.error("Stripe ödeme hatası: {}", e.getMessage());
            throw new PaymentException("Stripe ödeme işlemi başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Stripe webhook imzasını doğrular
     * GÜVENLİK: Webhook'ların gerçekten Stripe'dan geldiğini doğrular
     */
    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        try {
            com.stripe.net.Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
            return true;
        } catch (Exception e) {
            log.error("Stripe webhook imza doğrulama hatası: {}", e.getMessage());
            return false;
        }
    }
}
