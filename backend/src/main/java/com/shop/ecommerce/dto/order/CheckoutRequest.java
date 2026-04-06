package com.shop.ecommerce.dto.order;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long userId;
    private String shippingAddress;
    private String paymentMethod; // CREDIT_CARD, CASH_ON_DELIVERY
    private Integer installments; // 1, 3, 6, 9
    private Long cardId; // optional: existing card ID
}
