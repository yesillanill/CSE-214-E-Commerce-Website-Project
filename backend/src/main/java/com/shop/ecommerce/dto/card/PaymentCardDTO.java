package com.shop.ecommerce.dto.card;

import lombok.Data;

@Data
public class PaymentCardDTO {
    private Long id;
    private String cardHolderName;
    private String maskedCardNumber; // **** **** **** 1234
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardType;
}
