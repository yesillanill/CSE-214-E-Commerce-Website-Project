// PayPal Sandbox ödeme sağlayıcı yapılandırması
// GÜVENLİK: Client ID ve Secret application.properties'ten okunur, koda gömülmez
// mode: "sandbox" (test) veya "live" (gerçek) ortam seçimi
package com.shop.ecommerce.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PayPalConfig {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode; // sandbox veya live

    // PayPal API base URL'ini environment'a göre döndür
    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }
}
