package com.shop.ecommerce.dto.product;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDTO {
    private String name;
    private String description;
    private Double price;
    private String img;
    private int stock;
}
