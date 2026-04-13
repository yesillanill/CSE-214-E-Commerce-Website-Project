// Ödeme durumu enum'u
// PENDING: beklemede, SUCCESS: başarılı, COMPLETED: tamamlandı (eski kayıtlar için),
// FAILED: başarısız, REFUNDED: iade edildi, AWAITING_DELIVERY: teslimat bekleniyor (kapıda ödeme)
package com.shop.ecommerce.enums;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    COMPLETED,
    FAILED,
    REFUNDED,
    AWAITING_DELIVERY
}
