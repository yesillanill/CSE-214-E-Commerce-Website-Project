package com.shop.ecommerce.dto.wishlist;

import com.shop.ecommerce.dto.product.ProductListDTO;
import lombok.Data;

@Data
public class WishlistItemResponse {
    private Long id;
    private ProductListDTO product;
}
