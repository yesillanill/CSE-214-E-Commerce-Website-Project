// Genel ödeme hatası exception'ı
// Stripe, PayPal, Coinbase entegrasyonlarında oluşan hatalarda kullanılır
package com.shop.ecommerce.exceptionHandler;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
