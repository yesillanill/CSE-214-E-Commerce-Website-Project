// Ödeme yöntemi enum'u
// Mevcut yöntemler korunarak yeni provider tabanlı yöntemler eklendi
package com.shop.ecommerce.enums;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    CASH_ON_DELIVERY,
    DIGITAL_WALLET,
    STRIPE,
    PAYPAL,
    CRYPTO
}
