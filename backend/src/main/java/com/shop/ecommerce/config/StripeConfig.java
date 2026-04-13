// Stripe ödeme sağlayıcı yapılandırması
// GÜVENLİK: API key application.properties'ten okunur, koda gömülmez
// Uygulama başlatıldığında Stripe SDK'ya API key atanır
package com.shop.ecommerce.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // Uygulama başlatıldığında Stripe SDK'ya secret key atanır
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
