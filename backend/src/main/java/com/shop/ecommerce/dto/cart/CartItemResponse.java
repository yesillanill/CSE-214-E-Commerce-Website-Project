package com.shop.ecommerce.dto.cart;

import com.shop.ecommerce.dto.product.ProductListDTO;
import lombok.Data;

@Data
public class CartItemResponse {
    private Long id;
    private Integer quantity;
    private ProductListDTO product;
}
