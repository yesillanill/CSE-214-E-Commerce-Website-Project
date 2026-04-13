// Kapıda ödeme (Cash on Delivery) yapılandırması
// Maksimum ödeme tutarı limiti kontrol edilir
// Bu limit hem frontend hem backend tarafında uygulanır
package com.shop.ecommerce.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class CodConfig {

    @Value("${cod.max.amount}")
    private Double maxAmount;
}
