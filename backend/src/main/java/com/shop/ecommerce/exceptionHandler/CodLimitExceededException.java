// Kapıda ödeme tutar limiti aşımı hatası
// cod.max.amount değerini aşan siparişlerde fırlatılır
package com.shop.ecommerce.exceptionHandler;

public class CodLimitExceededException extends RuntimeException {
    public CodLimitExceededException(String message) {
        super(message);
    }
}
