package com.shop.ecommerce.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO {
    private Long id;
    private String name;
    private Double price;
    private double rating;
    private String img;
}
