// Coinbase Commerce kripto ödeme sağlayıcı yapılandırması
// GÜVENLİK: API key application.properties'ten okunur, koda gömülmez
package com.shop.ecommerce.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class CoinbaseConfig {

    @Value("${coinbase.api.key}")
    private String apiKey;

    // Coinbase Commerce API base URL'i
    private static final String BASE_URL = "https://api.commerce.coinbase.com";

    public String getBaseUrl() {
        return BASE_URL;
    }
}
