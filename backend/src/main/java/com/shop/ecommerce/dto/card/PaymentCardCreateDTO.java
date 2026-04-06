package com.shop.ecommerce.dto.card;

import lombok.Data;

@Data
public class PaymentCardCreateDTO {
    private Long userId;
    private String cardHolderName;
    private String cardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvv;
    private String cardType;
}
