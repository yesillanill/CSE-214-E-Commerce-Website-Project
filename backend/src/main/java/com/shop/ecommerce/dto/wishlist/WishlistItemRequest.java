package com.shop.ecommerce.dto.wishlist;

import lombok.Data;

@Data
public class WishlistItemRequest {
    private Long productId;
    private Long userId;
}
