package com.shop.ecommerce.dto.cart;

import lombok.Data;

@Data
public class CartItemRequest {
    private Long productId;
    private Integer quantity;
    private Long userId;
}
