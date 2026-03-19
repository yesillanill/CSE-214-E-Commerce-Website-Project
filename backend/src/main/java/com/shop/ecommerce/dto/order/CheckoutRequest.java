package com.shop.ecommerce.dto.order;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long userId;
    private String shippingAddress;
}
