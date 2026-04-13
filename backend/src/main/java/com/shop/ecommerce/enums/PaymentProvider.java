// Ödeme sağlayıcı türlerini tanımlayan enum
// Stripe, PayPal, Coinbase Commerce (Kripto) ve Kapıda Ödeme desteklenir
package com.shop.ecommerce.enums;

public enum PaymentProvider {
    STRIPE,
    PAYPAL,
    CRYPTO,
    COD
}
